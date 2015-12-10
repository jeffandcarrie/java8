package stream.api;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joda.time.format.DateTimeFormatterBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;



public class SeedData {
	static final String BASE_URL="http://localhost:8080/api/"; //local
//	static final String BASE_URL="http://acceleratordemo.elasticbeanstalk.com/api/"; //war file in test
	
	public static int queryForId(String filter, String type) throws Exception{
        CloseableHttpClient httpclient = HttpClients.createDefault();
        int id=-1;
        
        try {
            HttpGet httpget = new HttpGet(BASE_URL + type + "?" + filter);
            httpget.setHeader("content-type", "application/json");
    		Gson gson = new GsonBuilder().setPrettyPrinting().create();


            System.out.println("Executing request: " + httpget.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httpget);
            try {
                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                
//                String body = EntityUtils.toString(response.getEntity());
//                System.out.println(body);
                
                JsonReader reader = new JsonReader(new InputStreamReader( response.getEntity().getContent()));
                reader.beginArray();
                if(reader.hasNext()) {
	                reader.beginObject();
	                while(reader.hasNext()) {
	                	String name = reader.nextName();
	                	if("id".equals(name)) {
	                		id = reader.nextInt();
	                	}
	                	else {
	                		reader.skipValue();
	                	}
	                }
                }
            } finally {
                response.close();
            }
        } 
        catch(Exception e) {
        	System.err.println("Error connection " + e.getMessage());
        	e.printStackTrace();
        }
        finally {
            httpclient.close();
        }        
		
        return id;
		
	}	
	public static int saveAndGetId(Object data, String type) throws Exception{
        CloseableHttpClient httpclient = HttpClients.createDefault();
        int id=-1;
        
        try {
            HttpPost httppost = new HttpPost(BASE_URL + type);
            httppost.setHeader("content-type", "application/json");
            //2015-05-23T20:25:01Z
    		Gson gson = new GsonBuilder().setPrettyPrinting()
    		   .setDateFormat("yyyy-MM-dd'T00:00:00Z'").create();
System.out.println("sending " +gson.toJson(data) );
            StringEntity reqEntity = new StringEntity(gson.toJson(data));

            httppost.setEntity( reqEntity);

            System.out.println("Executing request: " + httppost.getRequestLine());
            System.out.println("    sending body: " + gson.toJson(data));
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                System.out.println("----------------------------------------");
                if(response.getStatusLine().getStatusCode()==500) {
                	System.out.println(gson.toJson(data));
                }
                
                System.out.println(response.getStatusLine());
                
                String body = EntityUtils.toString(response.getEntity());
                System.out.println(body);
                
                Map<String,Object> foo = new HashMap<String,Object>();
                foo = gson.fromJson(body, foo.getClass());
                System.out.println(foo);
                Double d = (Double)foo.get("id");
                id = d.intValue();
            } finally {
                response.close();
            }
        } 
        catch(Exception e) {
        	System.err.println("Error connection " + e.getMessage());
        	e.printStackTrace();
        }
        finally {
            httpclient.close();
        }        
		
        return id;
	}
	
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
	
	private static List<LicenseDetail> getDetails(List <CSVRecord> records) throws Exception{
		SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
		DateTimeFormatter dateFmt = (new java.time.format.DateTimeFormatterBuilder()).appendPattern("M/d/y").toFormatter();
		
		List <LicenseDetail> licDetails = new ArrayList<LicenseDetail>();
		for(CSVRecord rec : records) {
			LicenseDetail detail = new LicenseDetail();
			detail.lineOfAuthority = rec.get("licenseDetail.loa");
			try {
				detail.startDate = fmt.parse (rec.get("licenseDetail.effectiveDate"));
				 LocalDate testDate = LocalDate.parse(rec.get("licenseDetail.effectiveDate"), dateFmt);
				detail.endDate = fmt.parse(rec.get("licenseDetail.expiration"));
			}
			catch(ParseException e) {
				
			}
			licDetails.add(detail);
		}
		return licDetails;
	}

	private static List <License> getLicense(List <CSVRecord> records, Producer p, Company ag) throws Exception {
		Map <String, List<CSVRecord>> grouped = records.stream()
				 .filter(r -> p.nipr.equals(r.get("producer.nipr")))					 
				 .collect(Collectors.groupingBy(drec->drec.get("license.licenseNumber") + drec.get("license.state")));

//		p.licenses = new ArrayList<License>();
		for(String group : grouped.keySet()) {
			List<CSVRecord> details = grouped.get(group);
			
			CSVRecord lic = details.get(0);
			License license = new License(p);
			license.licenseNumber = lic.get("license.licenseNumber");
// double check this			license.licenseType = lic.get("license.type");
			license.licenseType = lic.get("license.resident");
			license.state = lic.get("license.state");
			license.licenseDetails = getDetails(details);
			
			

			StateDescription desc = new StateDescription();
			desc.stateDescription = lic.get("license.stateLicense");
			int descId = queryForId("stateDescription=" + java.net.URLEncoder.encode(desc.stateDescription), "producerStateDescription");
			if(descId == -1) {
				descId = saveAndGetId(desc, "producerStateDescription");
				
			}
			
			license.stateDescription = new Ref(descId);
			
			
			int id = saveAndGetId(license, "prodLicenses");
			license.id=id;
			
			
//			p.licenses.add(license);
		}	
		return null;
		
	}
	
	private static List <TpaLicense> getLicense(List <CSVRecord> records, Tpa p, Company ag) throws Exception {
		Map <String, List<CSVRecord>> grouped = records.stream()
				 .filter(r -> p.npnNumber.equals(r.get("tpa.npn")))					 
				 .collect(Collectors.groupingBy(drec->drec.get("license.licenseNumber") + drec.get("license.state")));

//		p.licenses = new ArrayList<License>();
		for(String group : grouped.keySet()) {
			List<CSVRecord> details = grouped.get(group);
			
			CSVRecord lic = details.get(0);
			TpaLicense license = new TpaLicense(p);
			license.licenseNumber = lic.get("license.licenseNumber");
			license.state = lic.get("license.state");
			license.complaintContact = lic.get("license.complaint_contact");
			license.licenseDetails = getDetails(details);
			
			
			int id = saveAndGetId(license, "tpaLicenses");
			license.id=id;
			
			
//			p.licenses.add(license);
		}	
		return null;
		
	}
	
	private static List<Producer> getProducers(List<CSVRecord> records, Company a) throws Exception{
		Map <String, List<CSVRecord>> producers = records.stream()
				 .collect(Collectors.groupingBy(rec->rec.get("producer.nipr")));

		List <Producer> plist = new ArrayList<Producer>();
		for(String producerID : producers.keySet()) {
			Producer p = new Producer(a);
			CSVRecord rec = producers.get(producerID).get(0);
			p.name = rec.get("producer.name");
			p.nipr = rec.get("producer.nipr");
			p.producerType = rec.get("pe.type");
			
			plist.add(p);

			int id = saveAndGetId(p, "producers");
			p.id=id;
			getLicense(records, p, a);
			
		}
		return plist;
		
	}
	

	private static List<Tpa> getTpas(List<CSVRecord> records, Company a) throws Exception{
		Map <String, List<CSVRecord>> tpas = records.stream()
				 .collect(Collectors.groupingBy(rec->rec.get("tpa.name")));

		List <Tpa> plist = new ArrayList<Tpa>();
		for(String tpaID : tpas.keySet()) {
			Tpa tpa = new Tpa(a);
			CSVRecord rec = tpas.get(tpaID).get(0);
			tpa.name = rec.get("tpa.name");
			tpa.npnNumber = rec.get("tpa.npn");
			plist.add(tpa);

			int id = saveAndGetId(tpa, "thirdPartyAdministrators");
			tpa.id=id;
			getLicense(records, tpa, a);
			
		}
		return plist;
		
	}
	
	
	
	
	private static void getAgency(String file, List<Company> agencyList,List<Client> clientList, LicenseeType type) throws Exception{
		Reader in = new FileReader(file);
		
		List <CSVRecord> records = StreamSupport.stream(CSVFormat.EXCEL.withHeader().withIgnoreSurroundingSpaces().parse(in)
										.spliterator(),false)
										.collect(Collectors.toList());

		CSVRecord agencyRecord = records.get(0);
	
		Optional<Company> agOpt =agencyList.stream()
										   .filter(a->a.EIN.equals(agencyRecord.get("pe.EIN")))
										   .findFirst();
		Optional<Client> cliOpt =clientList.stream()
										   .filter(a->a.name .equals(agencyRecord.get("client.name")))
										   .findFirst();

		agencyList.stream().forEach(a->System.out.println("LIST HASE: (" + a.EIN + ")"));
		System.out.println("LOOK FOR (" + agencyRecord.get("pe.EIN") + ")" + agOpt.isPresent());
		
		
		Company ag = null;
		
		if(agOpt.isPresent() ) {
			ag = agOpt.get();
//			ag.producers.addAll(getProducers(records, ag));
			
			getProducers(records, ag);
		}
		else {
			ag = new Company();
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
				ag.client = new Ref(client.id);
			}
			
			int id = saveAndGetId(ag, "companies");
			ag.id=id;

			
			switch(type) {
			case PRODUCER :
				getProducers(records, ag);
				break;
			case TPA :
				getTpas(records, ag);
				System.out.println("LOAD THE TPA");
				break;
			}

		}
	}

	
	public static void main(String [] args) throws Exception{
		List <Client> clients = new ArrayList<Client>();
		List <Company> agencies = new ArrayList<Company>();
		

//		getClient("bin/spc-v2.csv", clients);
//		getClient("bin/spc-hoppes.csv", clients); 
//		getClient("bin/spc-nguyen.csv", clients); 
//		getClient("bin/cordogan-v2.csv", clients); 
//		getClient("bin/sample-tpa-new.csv", clients); 
		//getClient("bin/00010113.csv", clients); 
//		getClient("bin/FGLoadFile.csv", clients);
		
		// new
//		getClient("bin/FGLicensing.csv", clients);
		
//		getClient("bin/Travelers-Travelers.csv", clients);
//		

		getClient("bin/Sample-Load-File-Sample-TPA.csv", clients); 	
		getClient("bin/Amwins-Load-File-Amwins-TPA.csv", clients);
//		getClient("bin/Amwins-Load-File-Amwins-Producer.csv", clients);
		getClient("bin/Cordogan-Load-File-M-Cordogan-Producer.csv", clients);
		getClient("bin/SPC-Load-File-Travelers-Client.csv", clients);

		for(Client c : clients) {
			int id = saveAndGetId(c, "clients");
			c.id = id;
			
		}

//		getAgency("bin/FGLicensing.csv", agencies, clients, LicenseeType.PRODUCER);
		


		getAgency("bin/Sample-Load-File-Sample-TPA.csv", agencies, clients, LicenseeType.TPA);
		getAgency("bin/Amwins-Load-File-Amwins-TPA.csv", agencies, clients, LicenseeType.TPA);
//		getAgency("bin/Amwins-Load-File-Amwins-Producer.csv", agencies, clients, LicenseeType.PRODUCER);
		getAgency("bin/Cordogan-Load-File-Cordogan-Agency-Producer.csv", agencies, clients, LicenseeType.PRODUCER);
		getAgency("bin/Cordogan-Load-File-M-Cordogan-Producer.csv", agencies, clients, LicenseeType.PRODUCER);
		getAgency("bin/SPC-Load-File-SPC-Producer.csv", agencies, clients, LicenseeType.PRODUCER);
		getAgency("bin/SPC-Load-File-Rose-Producer.csv", agencies, clients, LicenseeType.PRODUCER);
		getAgency("bin/SPC-Load-File-Nguyen-Producer.csv", agencies, clients, LicenseeType.PRODUCER);
		getAgency("bin/SPC-Load-File-Brenya-Producer.csv", agencies, clients, LicenseeType.PRODUCER);
		// end new
		
		// old
		//getAgency("bin/Sample-TPA-Load-File-fields-v2.csv", agencies, clients, LicenseeType.TPA);
//		getAgency("bin/FGLoadFile.csv", agencies, clients, LicenseeType.PRODUCER);
		
//		getAgency("bin/spc-v2.csv", agencies, clients, LicenseeType.PRODUCER); 
//		getAgency("bin/spc-hoppes.csv", agencies, clients,LicenseeType.PRODUCER ); 
//		getAgency("bin/spc-nguyen.csv", agencies, clients,LicenseeType.PRODUCER); 
//		getAgency("bin/cordogan-v2.csv", agencies,clients,LicenseeType.PRODUCER); 
//		getAgency("bin/sample-tpa-new.csv", agencies,clients,LicenseeType.TPA); 
//		getAgency("bin/00010113.csv", agencies, clients,LicenseeType.PRODUCER); 

	}
}
