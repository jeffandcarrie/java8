package stream;

/*
 * 
Link	Table 1 - Parent Name	Contact Name	Address	Telephone	Domicile State	Email	EIN	Table 2A Agency Name	Table 2B Type of License
	Cordogan Insurance Agency	Michael Cordogan	154 Timber Creek Dr., Ste. 2, Cordova, TN 38018	901-309-5585	TN	mike@farmersemail.com	899999999	Cordogan Insurance Agency	Producer
 * 
 */

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


class Client {
	String id= UUID.randomUUID().toString();
	String name;
	
}

class ProducerEntity {
	String id= UUID.randomUUID().toString();

	String name;
	String contactName;
	String address1;
	String phone;
	String contactEmail;
	String domicileState;
	String EIN;
	
	String clientName;
	String clientId;
	List <Producer> producers;
}
class Producer {
	String id= UUID.randomUUID().toString();
	String name;
	String nipr;
	String producerType;

	String agencyId;
	String agencyName;
	public Producer(ProducerEntity a) {
		this.agencyId=a.id;
		this.agencyName=a.name;
	}
	
	List <License> licenses;
}

class License {
	String id= UUID.randomUUID().toString();
	String state;
	String licenseNumber;
	String licenseType;
	String stateLicense;

	String agencyId;
	String agencyName;
	String producerId;
	String producerName;

	public License(ProducerEntity a, Producer p) {
		this.agencyId=a.id;
		this.agencyName=a.name;
		this.producerId=p.id;
		this.producerName=p.name;
	}

	List <LicenseDetail> licenseDetails;
}
class LicenseDetail {
	String id= UUID.randomUUID().toString();
	String lineOFAuthority;
	String startDate;
	String endDate;
}

public class ReadCSV {
	
	
	
	private static List<Producer> getProducers(List<CSVRecord> records, ProducerEntity a) {
		Map <String, List<CSVRecord>> producers = records.stream()
				 .collect(Collectors.groupingBy(rec->rec.get("producer.nipr")));

		List <Producer> plist = new ArrayList<Producer>();
		for(String producerID : producers.keySet()) {
			Producer p = new Producer(a);
			CSVRecord rec = producers.get(producerID).get(0);
			p.name = rec.get("producer.name");
			p.nipr = rec.get("producer.nipr");
			p.producerType = rec.get("pe.type");
			
			p.licenses = getLicense(records, p, a);
			plist.add(p);
			
		}
		return plist;
		
	}
	
	private static List <License> getLicense(List <CSVRecord> records, Producer p, ProducerEntity ag) {
		Map <String, List<CSVRecord>> grouped = records.stream()
				 .filter(r -> p.nipr.equals(r.get("producer.nipr")))					 
				 .collect(Collectors.groupingBy(drec->drec.get("license.licenseNumber") + drec.get("license.state")));

		p.licenses = new ArrayList<License>();
		for(String group : grouped.keySet()) {
			List<CSVRecord> details = grouped.get(group);
			
			CSVRecord lic = details.get(0);
			License license = new License(ag, p);
			license.licenseNumber = lic.get("license.licenseNumber");
			license.licenseType = lic.get("license.type");
			license.state = lic.get("license.state");
			license.stateLicense = lic.get("license.stateLicense");
			license.licenseDetails = getDetails(details);
			
			p.licenses.add(license);
		}	
		return p.licenses;
		
	}
	
	private static List<LicenseDetail> getDetails(List <CSVRecord> records) {
		List <LicenseDetail> licDetails = new ArrayList<LicenseDetail>();
		for(CSVRecord rec : records) {
			LicenseDetail detail = new LicenseDetail();
			detail.lineOFAuthority = rec.get("licenseDetail.loa");
			detail.startDate = rec.get("licenseDetail.effectiveDate");
			detail.endDate = rec.get("licenseDetail.expiration");
			licDetails.add(detail);
		}
		return licDetails;
	}
	


	/**
	 * link
	 * client.name	
	 * pe.contact	
	 * pe.address	
	 * pe.phone	
	 * pe.state	
	 * pe.email	
	 * pe.EIN	
	 * pe.name	
	 * pe.type	
	 * producer.name	
	 * producer.nipr	
	 * license.resident	
	 * license.state	
	 * license.licenseNumber	
	 * license.type	
	 * license.stateLicense	
	 * licenseDetail.loa	
	 * licenseDetail.effectiveDate	
	 * licenseDetail.expiration	
	 * Notes		
	 */
	

	private static void getClient(String file, List<Client> clientList) throws Exception{
		Reader in = new FileReader(file);
		
		List <CSVRecord> records = StreamSupport.stream(CSVFormat.EXCEL.withHeader().withIgnoreSurroundingSpaces().parse(in)
				.spliterator(),false)
				.collect(Collectors.toList());

		CSVRecord clientRecord = records.get(0);
		Optional<Client> cliOpt =clientList.stream().filter(a->a.name .equals(clientRecord.get("client.name"))).findFirst();

		if(!cliOpt.isPresent()) {
			Client c = new Client();
			c.name=clientRecord.get("client.name");
			clientList.add(c);
		}
		
	}
	
	private static void getAgency(String file, List<ProducerEntity> agencyList,List<Client> clientList) throws Exception{
		Reader in = new FileReader(file);
		
		List <CSVRecord> records = StreamSupport.stream(CSVFormat.EXCEL.withHeader().withIgnoreSurroundingSpaces().parse(in)
										.spliterator(),false)
										.collect(Collectors.toList());

		CSVRecord agencyRecord = records.get(0);
	
		Optional<ProducerEntity> agOpt =agencyList.stream().filter(a->a.EIN.equals(agencyRecord.get("pe.EIN"))).findFirst();
		Optional<Client> cliOpt =clientList.stream().filter(a->a.name .equals(agencyRecord.get("client.name"))).findFirst();

		agencyList.stream().forEach(a->System.out.println("LIST HASE: (" + a.EIN + ")"));
		System.out.println("LOOK FOR (" + agencyRecord.get("pe.EIN") + ")" + agOpt.isPresent());
		
		
		ProducerEntity ag = null;
		
		if(agOpt.isPresent() ) {
			ag = agOpt.get();
			ag.producers.addAll(getProducers(records, ag));
		}
		else {
			ag = new ProducerEntity();
			agencyList.add(ag);
			
			ag.name = agencyRecord.get("pe.name");
			ag.address1 = agencyRecord.get("pe.address");
			ag.contactName = agencyRecord.get("pe.contact");
			ag.phone = agencyRecord.get("pe.phone");
			ag.domicileState = agencyRecord.get("pe.state");		
			ag.contactEmail = agencyRecord.get("pe.email");
			ag.EIN = agencyRecord.get("pe.EIN");

			if(cliOpt.isPresent()) {
				Client client = cliOpt.get();
				ag.clientId=client.id;
				ag.clientName=client.name;
			}
			ag.producers = getProducers(records, ag);

		}
	}
	public static void main(String [] args) throws Exception {
		List <ProducerEntity> agencies = new ArrayList<ProducerEntity>();
		List <Client> clients = new ArrayList<Client>();
		

		getClient("bin/spc-v2.csv", clients); 
		getClient("bin/spc-hoppes.csv", clients); 
		getClient("bin/spc-nguyen.csv", clients); 
		getClient("bin/cordogan-v2.csv", clients); 
		
		
		getAgency("bin/spc-v2.csv", agencies, clients); 
		getAgency("bin/spc-hoppes.csv", agencies, clients ); 
		getAgency("bin/spc-nguyen.csv", agencies, clients); 
		getAgency("bin/cordogan-v2.csv", agencies,clients); 
		
		
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json =gson.toJson(agencies);


		Files.createDirectory(Paths.get("./temp/producerEntity"));
		Files.createDirectory(Paths.get("./temp/producer"));
		Files.createDirectory(Paths.get("./temp/license"));
		Files.createDirectory(Paths.get("./temp/licenseDetail"));
		Files.createDirectory(Paths.get("./temp/client"));
		
		Files.write( Paths.get("./temp/producerEntity/producerEntities.json"), json.getBytes() ,StandardOpenOption.CREATE);
		for(ProducerEntity agency : agencies) {
			json =gson.toJson(agency);
			Files.write( Paths.get("./temp/producerEntity/" + agency.id +".json"), json.getBytes() ,StandardOpenOption.CREATE);
			
			for(Producer producer : agency.producers) {
				json =gson.toJson(producer);
				Files.write( Paths.get("./temp/producer/" + producer.id +".json"), json.getBytes() ,StandardOpenOption.CREATE);

				for(License license : producer.licenses)  {
					json =gson.toJson(license);
					Files.write( Paths.get("./temp/license/" + license.id +".json"), json.getBytes() ,StandardOpenOption.CREATE);
					
				}
			}
		}

		
		json =gson.toJson(clients);
		Files.write( Paths.get("./temp/client/clients.json"), json.getBytes() ,StandardOpenOption.CREATE);

		System.out.println(json);
		

		
		
/*		
		int count = 0;
		for(String group : grouped.keySet()) {
			System.out.println(group);
			
			
			count += grouped.get(group).size();
			grouped.get(group).forEach(a ->System.out.println("   " + a.get("LineOfAuthority")));
			
		}
		System.out.println("total count " + count + "  group count " + grouped.keySet().size());
	*/	
	}
}
