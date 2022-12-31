package si.fri.rso.skupina1.optimizator.lib;

public class User {

	private Integer userId;
	private String name;
	private String surname;
	private String city;
	private Integer postalCode;
	private String address;
	private Boolean canDeliver;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public Integer getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(Integer postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Boolean getCanDeliver() {
		return canDeliver;
	}

	public void setCanDeliver(Boolean canDeliver) {
		this.canDeliver = canDeliver;
	}
}
