package stream.api;

import java.util.Date;
import java.util.List;

import com.google.gson.annotations.SerializedName;



class StateLicense {
	String name;
}



class Client {
	Integer id;
	String name;
}

class Company {
	Integer id;
	
	String name;
	String contactName;
	String address1;
	String phone;
	String contactEmail;
	String domicileState;
	String EIN;
	
	Ref client;
//	List <Producer> producers;
}


class License {
	Integer id;
	
	String state;
	String licenseNumber;
	String licenseType;
	String resident;

	Ref producer;
	Ref stateDescription;
	
	public License(Producer p) {
		this.producer = new Ref(p.id);
	}

	List <LicenseDetail> licenseDetails;
}

class TpaLicense {
	Integer id;
	
	String state;
	String licenseNumber;
	String complaintContact;

	Ref tpa;
	
	public TpaLicense(Tpa p) {
		this.tpa = new Ref(p.id);
	}

	List <LicenseDetail> licenseDetails;
}


class LicenseDetail {
	@SerializedName("class")
	String className="com.accellawgroup.accelerator.domain.LicenseDetail";
	String lineOfAuthority ="N/A";
	Date startDate;
	Date endDate;
}


class Producer {
	Integer id;
	
	String name;
	String nipr ="N/a";
	String producerType;

	Ref company;

	public Producer(Company a) {
		company = new Ref(a.id);
	}
	
//	List <License> licenses;
}



class Tpa {
	Integer id;
	
	String name;
	String npnNumber;

	Ref company;

	public Tpa(Company a) {
		company = new Ref(a.id);
	}
	
}


class Ref {
	Integer id;
	public Ref(Integer id) {
		this.id=id;
	}
}

class StateDescription {
	String stateDescription;
}

enum LicenseeType {
	PRODUCER,
	TPA
}



class ToDo {
	public ToDo(int companyId, int typeId) {
		company = new Ref(companyId);
		type = new Ref(typeId);
	}
	
	Ref company;
	Ref type;
	String comments;
	Date dueDate;
	String status;	
	boolean deleted;
}

class ToDoCategory {
	String toDoCategory;
}