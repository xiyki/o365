package hqr.o365.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.web.client.RestTemplate;

import hqr.o365.dao.TaMasterCdRepo;
import hqr.o365.dao.TaOfficeInfoRepo;
import hqr.o365.domain.TaMasterCd;
import hqr.o365.domain.TaOfficeInfo;

@Service
public class DomainAction {
    @Autowired
    private SelectedOfficeInfo selectedOfficeInfo;
	
	@Autowired
	private TaMasterCdRepo tmc;
	
	@Autowired
	private TaOfficeInfoRepo repo;
	
	@Autowired
	private GetDomainInfo2 gdi2;
	
	@Autowired
	private ValidateAppInfo vai;
	
	@Autowired
	private CreateDnsRecordInCf cnr;
	
	private RestTemplate restTemplate = GraphHttpClient.create();
	
	@Value("${UA}")
    private String ua;

	private boolean validDomain(String domain) {
		return domain != null && domain.length() <= 253
				&& domain.matches("(?i)[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\\.[a-z]{2,63}");
	}
	
	@CacheEvict(value= {"cacheDomain"}, allEntries = true)
	public boolean createDomain(String domain) {
		boolean res = false;
		if (!validDomain(domain)) {
			return false;
		}
		
		String allDomains = gdi2.getAllDomains();
		if(allDomains.indexOf(domain+" - unverified")>=0) {
			System.out.println("old unverified domain, skip create domain action");
			return true;
		}
		
		List<TaOfficeInfo> list = selectedOfficeInfo.current();
		if(list!=null&&list.size()>0) {
			TaOfficeInfo ta = list.get(0);
			String accessToken = "";
			accessToken = vai.getToken(ta.getTenantId(), ta.getAppId(), ta.getSecretId());
			
			if(!"".equals(accessToken)) {
				String endpoint = "https://graph.microsoft.com/v1.0/domains";
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.USER_AGENT, ua);
				headers.add("Authorization", "Bearer "+accessToken);
				headers.setContentType(MediaType.APPLICATION_JSON);
				
				String json="{\"id\": \""+domain+"\"}";
				System.out.println(json);
				
				HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
				try {
					ResponseEntity<String> response= restTemplate.postForEntity(endpoint, requestEntity, String.class);

					if(response.getStatusCodeValue()==201) {
						res = true;
						System.out.println("Created domain "+domain+" done");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					if(e instanceof BadRequest) {
						
					}
				}
			}
		}
		
		return res;
	}
	
	@CacheEvict(value= {"cacheDomain"}, allEntries = true)
	public String verificationDnsRecords(String domain) {
		String dnsRecords = "fail";
		if (!validDomain(domain)) {
			return dnsRecords;
		}
		
		List<TaOfficeInfo> list = selectedOfficeInfo.current();
		if(list!=null&&list.size()>0) {
			TaOfficeInfo ta = list.get(0);
			String accessToken = "";
			accessToken = vai.getToken(ta.getTenantId(), ta.getAppId(), ta.getSecretId());
			
			if(!"".equals(accessToken)) {
				String endpoint = "https://graph.microsoft.com/v1.0/domains/"+domain+"/verificationDnsRecords";
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.USER_AGENT, ua);
				headers.add("Authorization", "Bearer "+accessToken);
				headers.setContentType(MediaType.APPLICATION_JSON);
				
				String json="";
				
				HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
				try {
					ResponseEntity<String> response= restTemplate.exchange(endpoint, HttpMethod.GET, requestEntity, String.class);

					if(response.getStatusCodeValue()==200) {
						dnsRecords = response.getBody();
						System.out.println("Get verfiy dnsrecord:"+dnsRecords);
						
						//automatic create DNS record in CF and verfiy
						String res = autoProcessInCF(dnsRecords);
						if("succ".equals(res)) {
							//Try 4 times verify DNS record
							for(int i=0;i<4;i++) {
								Thread.sleep(2000);
								System.out.println((i+1)+" time, try to verify domain:"+domain);
								String res2 = verifyDomain(domain);
								if("succ".equals(res2)) {
									dnsRecords = "succ";
									break;
								}
							}
						}
					}
				}
				catch (Exception e) {
					System.out.println(e.toString());
				}
			}
		}
		
		return dnsRecords;
	}
	
	@CacheEvict(value= {"cacheDomain"}, allEntries = true)
	public String verifyDomain(String domain) {
		String res = "fail";
		if (domain == null) {
			return res;
		}
		String arr[] = domain.split(" - ");
		if (!validDomain(arr[0])) {
			return res;
		}
		
		List<TaOfficeInfo> list = selectedOfficeInfo.current();
		if(list!=null&&list.size()>0) {
			TaOfficeInfo ta = list.get(0);
			String accessToken = "";
			accessToken = vai.getToken(ta.getTenantId(), ta.getAppId(), ta.getSecretId());
			
			if(!"".equals(accessToken)) {
				String endpoint = "https://graph.microsoft.com/v1.0/domains/"+arr[0]+"/verify";
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.USER_AGENT, ua);
				headers.add("Authorization", "Bearer "+accessToken);
				headers.setContentType(MediaType.APPLICATION_JSON);
				
				String json="";
				
				HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
				try {
					ResponseEntity<String> response= restTemplate.postForEntity(endpoint, requestEntity, String.class);

					if(response.getStatusCodeValue()==200) {
						res = "succ";
						System.out.println("domain "+domain+" verified");
					}
				}
				catch (Exception e) {
					System.out.println(e.toString());
				}
			}
		}
		
		return res;
	}
	
	@CacheEvict(value= {"cacheDomain"}, allEntries = true)
	public boolean deleteDomain(String domain) {
		boolean res = false;
		
		if (domain == null) {
			return false;
		}
		String arr[] = domain.split(" - ");
		if (!validDomain(arr[0]) || arr[0].toLowerCase().endsWith(".onmicrosoft.com")) {
			return false;
		}
		
		List<TaOfficeInfo> list = selectedOfficeInfo.current();
		if(list!=null&&list.size()>0) {
			TaOfficeInfo ta = list.get(0);
			String accessToken = "";
			accessToken = vai.getToken(ta.getTenantId(), ta.getAppId(), ta.getSecretId());
			
			if(!"".equals(accessToken)) {
				String endpoint = "https://graph.microsoft.com/v1.0/domains/"+arr[0]+"/forceDelete";
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.USER_AGENT, ua);
				headers.add("Authorization", "Bearer "+accessToken);
				headers.setContentType(MediaType.APPLICATION_JSON);
				
				String json="";
				
				HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
				try {
					ResponseEntity<String> response= restTemplate.postForEntity(endpoint, requestEntity, String.class);

					if(response.getStatusCodeValue()==204) {
						res = true;
						System.out.println("Deleted domain "+domain+" done");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return res;
	}

	public String autoProcessInCF(String dnsRecords) {
		String res = "fail";
		
		Optional<TaMasterCd> opt1 = tmc.findById("CF_AUTH_EMAIL");
		Optional<TaMasterCd> opt2 = tmc.findById("CF_AUTH_KEY");

		if(opt1.isPresent()&&opt2.isPresent()) {
			TaMasterCd en1 = opt1.get();
			TaMasterCd en2 = opt2.get();
			if(en1.getCd()!=null&&!"".equals(en1.getCd())&&en2.getCd()!=null&&!"".equals(en2.getCd())) {
				String cfAuthEmail = en1.getCd();
				String cfAuthKey = en2.getCd();
				
				JSONObject jb = JSON.parseObject(dnsRecords);
				JSONArray ja = jb.getJSONArray("value");

				JSONObject tmp1 = new JSONObject();
				for(int i=0;i<ja.size();i++) {
					tmp1 = ja.getJSONObject(i);
					if("TXT".equalsIgnoreCase(tmp1.getString("recordType"))) {
						break;
					}
				}
				
				res = cnr.create(tmp1.getString("label"), tmp1.getString("recordType"), tmp1.getString("ttl"), tmp1.getString("text"), cfAuthEmail, cfAuthKey);
				
			}
			else {
				System.out.println("CF_AUTH_EMAIL or CF_AUTH_KEY value is null");
			}
		}
		else {
			System.out.println("CF_AUTH_EMAIL or CF_AUTH_KEY do not exist");
		}
		return res;
	}
	
}
