package si.fri.rso.skupina1.optimizator.services.clients;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import javax.enterprise.context.Dependent;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import java.util.concurrent.CompletionStage;

@Path("forward")
@RegisterRestClient(configKey = "geocoding-api")
@Dependent
@ClientHeaderParam(name = "X-RapidAPI-Key", value = "{getApiKey}")
@ClientHeaderParam(name = "X-RapidAPI-Host", value = "{getApiHost}")
public interface GeocodingApi {

	@GET
	CompletionStage<String> geocodingAsync(@QueryParam("street") String street, @QueryParam("country") String country);

	@GET
	String geocoding(@QueryParam("street") String street, @QueryParam("country") String country);

	default String getApiKey() {
		return ConfigurationUtil.getInstance().get("integrations.geocoding.apiKey").get();
	}

	default String getApiHost() {
		return ConfigurationUtil.getInstance().get("integrations.geocoding.apiHost").get();
	}
}