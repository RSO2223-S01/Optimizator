package si.fri.rso.skupina1.optimizator.api.v1.resources;

import com.kumuluz.ee.logs.cdi.Log;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import si.fri.rso.skupina1.optimizator.lib.Order;
import si.fri.rso.skupina1.optimizator.services.clients.GeocodingApi;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.UUID;

@Log
@ApplicationScoped
@Path("/optimize")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OptimizatorResource {

	private Logger log = Logger.getLogger(OptimizatorResource.class.getName());

	@Context
	protected UriInfo uriInfo;

	private static final UUID uuid = UUID.randomUUID();

	private Client httpClient;
	private String baseUrlUsers;
	private String baseUrlOrders;

	@Inject
	@RestClient
	private GeocodingApi geocodingApi;

	@PostConstruct
	private void init() {
		httpClient = ClientBuilder.newClient();
		baseUrlUsers = ConfigurationUtil.getInstance().get("integrations.uporabniki.baseurl").get();
		baseUrlOrders = ConfigurationUtil.getInstance().get("integrations.narocila.baseurl").get();
		log.info("Initialized: " + OptimizatorResource.class.getName() + ", UUID: " + uuid);
	}

	private String executePOST(String body, String baseUrl) {
		MediaType APPLICATION_GRAPHQL = new MediaType("application", "graphql");
		return httpClient
				.target(baseUrl)
				.request(APPLICATION_GRAPHQL)
				.post(Entity.entity(body, APPLICATION_GRAPHQL))
				.readEntity(String.class);
	}

	@Counted(name = "optimizeOrders_total")
	@Metered(name = "optimizeOrders_rate")
	@Operation(description = "Get orders that match the criteria.", summary = "Get orders")
	@Path("/{userId}")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "List of orders", content = @Content(schema = @Schema(implementation = Order.class, type = SchemaType.ARRAY)), headers = {
					@Header(name = "X-Total-Count", description = "Number of objects in list") }) })
	@GET
	public void asyncGetOrders(
			@Parameter(description = "User ID.", required = true) @PathParam("userId") Integer userId,
			@Parameter(description = "Minimum client rating", required = false) @QueryParam("minRating") Double minRating,
			@Parameter(description = "Maximum distance to dropoff", required = false) @QueryParam("maxDist") Double maxDist,
			@Suspended final AsyncResponse asyncResponse) {
		final double minRating_ = minRating == null ? 0.0 : minRating;
		final double maxDist_ = maxDist == null ? Double.POSITIVE_INFINITY : maxDist;
		String graphQLquery = """
				query MyQuery {
					allOrders(filter: {fields: {op: EQ, field: "status", value: "OPEN"}}) {
						result {
						address
						clientId
						clientScore
						comment
						deliveryPersonId
						deliveryScore
						id
						status
						}
					}
				}
				""";
		String res = executePOST(graphQLquery, baseUrlOrders);
		// log.info("ORDERS: >>> " + res + "<<<");
		JSONObject responseDataJSON = new JSONObject(res).getJSONObject("data");
		JSONArray ordersJSON = responseDataJSON.getJSONObject("allOrders").getJSONArray("result");
		// log.info("Order array" + ordersJSON.toString());
		graphQLquery = """
				query MyQuery {
					allUsers(filter: {fields: {op: EQ, field: "userId", value: "%id"}}) {
						result {
						address
						postalCode
						city
						}
					}
				}
				""".replace("%id", "" + userId);
		res = executePOST(graphQLquery, baseUrlUsers);
		// log.info("USER ADDRESS: >>> " + res + "<<<");
		responseDataJSON = new JSONObject(res).getJSONObject("data");
		JSONArray usersJSON = responseDataJSON.getJSONObject("allUsers").getJSONArray("result");
		JSONObject userJSON = usersJSON.getJSONObject(0);
		// log.info("User" + userJSON.toString());
		// log.info("Calling google API");
		CompletableFuture<String> stringCompletionStage = geocodingApi
				.geocodingAsync(userJSON.getString("address"), "Slovenia")
				.toCompletableFuture();
		stringCompletionStage.whenComplete((s, throwable) -> {
			try {
				// log.info("Inside");
				double R = 6371;
				JSONObject response = new JSONArray(s).getJSONObject(0);
				// log.info(response.toString());
				double lat = Math.toRadians(response.getDouble("lat"));
				double lon = Math.toRadians(response.getDouble("lon"));
				ArrayList<JSONObject> filtered = new ArrayList<>();
				for (int i = 0; i < ordersJSON.length(); i++) {
					// log.info(ordersJSON.getJSONObject(i).getString("address"));
					String resp = geocodingApi.geocoding(ordersJSON.getJSONObject(i).getString("address"), "Slovenia");
					// log.info(resp);
					response = new JSONArray(resp).getJSONObject(0);
					double lat_i = Math.toRadians(response.getDouble("lat"));
					double lon_i = Math.toRadians(response.getDouble("lon"));
					double sin1 = (lat_i - lat) / 2;
					double sin2 = (lon_i - lon) / 2;
					double distance = 2 * R
							* Math.asin(Math.sqrt(sin1 * sin1 + Math.cos(lat) * Math.cos(lat_i) * sin2 * sin2));
					if (distance < maxDist_)
						filtered.add(ordersJSON.getJSONObject(i));
					ordersJSON.getJSONObject(i).put("distance", distance);
				}
				ArrayList<JSONObject> finalOrders = new ArrayList<>();
				for (JSONObject order : filtered) {
					final String graphQLquery2 = """
							query MyQuery {
								allOrders(filter: {fields: {field: "clientId", op: EQ, value: "%id"}}) {
									result {
									clientScore
									}
								}
							}
							""".replace("%id", "" + userId);
					final String response2 = executePOST(graphQLquery2, baseUrlOrders);
					// log.info(response2);
					JSONArray userOrders = new JSONObject(response2).getJSONObject("data").getJSONObject("allOrders")
							.getJSONArray("result");
					Double average = userOrders.toList().stream().map(x -> ((JSONObject) x))
							.collect(Collectors.averagingDouble(x -> x.getLong("clientRating")));
					// log.info("" + average);
					if (average >= minRating_)
						finalOrders.add(order);
				}
				JSONArray finalArray = new JSONArray(finalOrders);
				// System.out.println(finalArray);
				asyncResponse.resume(finalArray.toString());
			} catch (Exception e) {
				e.printStackTrace();
				log.info(e.getMessage());
			}

		});
		stringCompletionStage.exceptionally(throwable -> {
			log.severe(throwable.getMessage());
			return throwable.getMessage();
		});
		// Response.status(Response.Status.OK).entity(users).build();
	}

}
