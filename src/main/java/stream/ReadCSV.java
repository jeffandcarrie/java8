package stream;

/*
 * 
Link	Table 1 - Parent Name	Contact Name	Address	Telephone	Domicile State	Email	EIN	Table 2A Agency Name	Table 2B Type of License
	Cordogan Insurance Agency	Michael Cordogan	154 Timber Creek Dr., Ste. 2, Cordova, TN 38018	901-309-5585	TN	mike@farmersemail.com	899999999	Cordogan Insurance Agency	Producer
 * 
 */

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
class Agency {
	String id= UUID.randomUUID().toString();
	String name;
	String contactName;
	String address1;
	String phone;
	String contactEmail;
	String domicileState;
	String EIN;
	
	List <Producer> producers;
}
class Producer {
	String id= UUID.randomUUID().toString();
	String name;
	String nipr;
	String producerType;

	String agencyId;
	String agencyName;
	public Producer(Agency a) {
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
	String agencyId;
	String agencyName;
	String producerId;
	String producerName;

	public License(Agency a, Producer p) {
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
	private static List<Producer> getProducers(List<CSVRecord> records, Agency a) {
		Map <String, List<CSVRecord>> producers = records.stream()
				 .collect(Collectors.groupingBy(rec->rec.get("NIPR")));

		List <Producer> plist = new ArrayList<Producer>();
		for(String producerID : producers.keySet()) {
			Producer p = new Producer(a);
			CSVRecord rec = producers.get(producerID).get(0);
			p.name = rec.get("Licensee");
			p.nipr = rec.get("NIPR");
			p.producerType = rec.get("ProducerType");
			plist.add(p);
			
		}
		return plist;
		
	}
	private static List<LicenseDetail> getDetails(List <CSVRecord> records) {
		List <LicenseDetail> licDetails = new ArrayList<LicenseDetail>();
		for(CSVRecord rec : records) {
			LicenseDetail detail = new LicenseDetail();
			detail.lineOFAuthority = rec.get("LineOfAuthority");
			detail.startDate = rec.get("EffectiveDate");
			detail.endDate = rec.get("ExpirationDate");
			licDetails.add(detail);
		}
		return licDetails;
	}
	
	private static Agency getAgency(String file, Agency ag) throws Exception{
		Reader in = new FileReader(file);
		
		List <CSVRecord> records = StreamSupport.stream(CSVFormat.EXCEL.withHeader().withIgnoreSurroundingSpaces().parse(in)
										.spliterator(),false)
										.collect(Collectors.toList());

		
		ag.producers = getProducers(records, ag);
		
		
		for(Producer p  : ag.producers) {
			Map <String, List<CSVRecord>> grouped = records.stream()
					 .filter(r -> p.nipr.equals(r.get("NIPR")))					 
					 .collect(Collectors.groupingBy(drec->drec.get("License") + drec.get("State")));

			p.licenses = new ArrayList<License>();
			for(String group : grouped.keySet()) {
				List<CSVRecord> details = grouped.get(group);
				
				CSVRecord lic = details.get(0);
				License license = new License(ag, p);
				license.licenseNumber = lic.get("License");
				license.licenseType = lic.get("LicenseType");
				license.state = lic.get("State");
				license.licenseDetails = getDetails(details);
				
				p.licenses.add(license);
			}			
		}
		return ag;
		
	}
	public static void main(String [] args) throws Exception {
		
		Agency cordigan = new Agency();
		cordigan.name="Cordogan Insurance Agency";
		cordigan.address1="154 Timber Creek Dr., Ste. 2, Cordova, TN 38018";
		cordigan.contactName="Michael Cordogan";
		cordigan.phone  ="901-309-5585";
		cordigan.domicileState="TN";
		cordigan.contactEmail="mike@farmersemail.com";	
		cordigan.EIN="899999999";
		
		Agency travellers = new Agency();
		travellers.name="The Travelers Companies, Inc.";
		travellers.address1="One Tower Square, CR10 Hartford, CT  06259";
		travellers.contactName="Kamal Agrawal";
		travellers.phone  ="(860) 954-9581";
		travellers.domicileState="CT";
		travellers.contactEmail="CT";	
		travellers.EIN="999999999";
		
		Agency [] agencies = {getAgency("bin/SPC.csv", travellers), 
				              getAgency("bin/Cordogan.csv", cordigan)};

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json =gson.toJson(agencies);


		Files.write( Paths.get("./temp/agencies.json"), json.getBytes() ,StandardOpenOption.CREATE);
		for(Agency agency : agencies) {
			json =gson.toJson(agency);
			Files.write( Paths.get("./temp/agency_" + agency.id +".json"), json.getBytes() ,StandardOpenOption.CREATE);
			
			for(Producer producer : agency.producers) {
				json =gson.toJson(producer);
				Files.write( Paths.get("./temp/producer_" + producer.id +".json"), json.getBytes() ,StandardOpenOption.CREATE);

				for(License license : producer.licenses)  {
					json =gson.toJson(license);
					Files.write( Paths.get("./temp/license_" + license.id +".json"), json.getBytes() ,StandardOpenOption.CREATE);
					
				}
			}
		}
		
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
