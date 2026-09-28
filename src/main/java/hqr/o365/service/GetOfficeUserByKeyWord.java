package hqr.o365.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import hqr.o365.dao.TaMasterCdRepo;
import hqr.o365.dao.TaOfficeInfoRepo;
import hqr.o365.domain.OfficeUser;
import hqr.o365.domain.TaMasterCd;
import hqr.o365.domain.TaOfficeInfo;

@Service
public class GetOfficeUserByKeyWord {
    @Autowired
    private SelectedOfficeInfo selectedOfficeInfo;

	private RestTemplate restTemplate = GraphHttpClient.create();
	
	@Autowired
	private TaOfficeInfoRepo repo;
	
	@Autowired
	private ValidateAppInfo vai;
	
	@Autowired
	private TaMasterCdRepo tmc;
	
	@Value("${UA}")
    private String ua;

	private URI searchUri(String base, String keyword, Integer rows) {
		String escaped = keyword.replace("'", "''");
		String filter = "startsWith(displayName,'" + escaped
				+ "') or startsWith(userPrincipalName,'" + escaped + "')";
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(base)
				.queryParam("$filter", filter);
		if (rows != null) {
			builder.queryParam("$select", "accountEnabled,usageLocation,id,userPrincipalName,displayName,assignedLicenses")
					.queryParam("$top", rows);
		}
		return builder.build().encode().toUri();
	}
	public HashMap<String, String> getUsers(int page, int rows, String keyword){
		HashMap<String, String> map = new HashMap<String, String>();
		List<OfficeUser> ll = new ArrayList<OfficeUser>();
		HashMap jsonTmp = new HashMap();
		
		List<TaOfficeInfo> list = selectedOfficeInfo.current();
		if(list!=null&&list.size()>0) {
			TaOfficeInfo ta = list.get(0);
			String accessToken = "";
			accessToken = vai.getToken(ta.getTenantId(), ta.getAppId(), ta.getSecretId());
			
			if(!"".equals(accessToken)) {
				URI endpoint = searchUri("https://graph.microsoft.com/v1.0/users/$count", keyword, null);
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.USER_AGENT, ua);
				headers.add("Authorization", "Bearer "+accessToken);
				headers.add("ConsistencyLevel", "eventual");
				String body="";
				HttpEntity<String> requestEntity = new HttpEntity<String>(body, headers);
				try {
					ResponseEntity<String> response= restTemplate.exchange(endpoint, HttpMethod.GET, requestEntity, String.class);
					String total = response.getBody();
					System.out.println("total user count is "+total);
					jsonTmp.put("total", total);
					int intTotal = Integer.parseInt(total);
					
					int totalPage = 1;
					if(intTotal%rows==0) {
						totalPage = intTotal/rows;
					}
					else {
						totalPage = intTotal/rows + 1;
					}
					
					if(page>totalPage&&totalPage!=0) {
						System.out.println("page "+page+ " is greater than totalPage, force it to "+totalPage);
						page = totalPage;
					}
					saveOfficeUserInList(map, ll, jsonTmp, accessToken, page, rows, keyword);
					
				}
				catch (Exception e) {
					e.printStackTrace();
					map.put("status", "1");
					map.put("message", "无法获得正确的状态或无法连接到microsoft 1");
				}
			}
			else {
				map.put("status", "1");
				map.put("message", "获取token失败，请确认全局的有效性");
			}
		}
		else {
			map.put("status", "1");
			map.put("message", "请先选择一个全局");
		}
		return map;
	}
	
	private void saveOfficeUserInList(HashMap<String, String> map, List<OfficeUser> ll, HashMap jsonTmp, String accessToken, int page, int rows, String keyword) {
		URI endpoint2 = searchUri("https://graph.microsoft.com/v1.0/users", keyword, rows);
		HttpHeaders headers2 = new HttpHeaders();
		headers2.set(HttpHeaders.USER_AGENT, ua);
		headers2.add("Authorization", "Bearer "+accessToken);
		String body2="";
		
		HttpEntity<String> requestEntity2 = new HttpEntity<String>(body2, headers2);
		try {
			ResponseEntity<String> response2= restTemplate.exchange(endpoint2, HttpMethod.GET, requestEntity2, String.class);
			if(response2.getStatusCodeValue()==200) {
				JSONObject jo = JSON.parseObject(response2.getBody());
				JSONArray ja = jo.getJSONArray("value");
				//1st page
				if(page==1) {
					for (Object object : ja) {
						OfficeUser ou = new OfficeUser();
						
						JSONObject jb = (JSONObject)object;
						String accountEnabled = jb.getString("accountEnabled");
						String usageLocation = jb.getString("usageLocation");
						String uid = jb.getString("id");
						String userPrincipalName = jb.getString("userPrincipalName");
						String displayName = jb.getString("displayName");
						JSONArray licen = jb.getJSONArray("assignedLicenses");
						if(licen!=null) {
							for (Object object2 : licen) {
								JSONObject newJb = (JSONObject)object2;
								String skuId = newJb.getString("skuId");
								
								//get decode value,else use skuid to display
								Optional<TaMasterCd> rlc = tmc.findById(skuId);
								if(rlc.isPresent()) {
									TaMasterCd skuenti = rlc.get();
									ou.getAssignedLicenses().add(skuenti.getDecode());
								}
								else {
									ou.getAssignedLicenses().add(skuId);
								}
							}
						}
						ou.setAccountEnabled(accountEnabled);
						ou.setUsageLocation(usageLocation);
						ou.setId(uid);
						ou.setUserPrincipalName(userPrincipalName);
						ou.setDisplayName(displayName);
						ll.add(ou);
					}
				}
				//other page
				else {
					String nextPage = jo.getString("@odata.nextLink");
					getNextUrl(nextPage, accessToken, page-1, ll);
				}
				jsonTmp.put("rows", ll);
				map.put("status", "0");
				map.put("message", JSON.toJSON(jsonTmp).toString());
			}
			else {
				map.put("status", "1");
				map.put("message", "在获取用户列表时未能获得正确的返回状态");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			map.put("status", "1");
			map.put("message", "无法获得正确的状态或无法连接到microsoft 2");
		}
	}
	
	private void getNextUrl(String url, String accessToken, int times, List<OfficeUser> ll) {
        if (url == null || !url.startsWith("https://graph.microsoft.com/")) {
            return;
        }
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.USER_AGENT, ua);
		headers.add("Authorization", "Bearer "+accessToken);
		String body="";
		
		HttpEntity<String> requestEntity = new HttpEntity<String>(body, headers);
		ResponseEntity<String> response= restTemplate.exchange(java.net.URI.create(url), HttpMethod.GET, requestEntity, String.class);
		
		JSONObject jo = JSON.parseObject(response.getBody());
		JSONArray ja = jo.getJSONArray("value");
		String nextPage = jo.getString("@odata.nextLink");
		
		times --;
		
		if(times<=0) {
			for (Object object : ja) {
				OfficeUser ou = new OfficeUser();
				JSONObject jb = (JSONObject)object;
				String accountEnabled = jb.getString("accountEnabled");
				String usageLocation = jb.getString("usageLocation");
				String uid = jb.getString("id");
				String userPrincipalName = jb.getString("userPrincipalName");
				String displayName = jb.getString("displayName");
				JSONArray licen = jb.getJSONArray("assignedLicenses");
				if(licen!=null) {
					for (Object object2 : licen) {
						JSONObject newJb = (JSONObject)object2;
						String skuId = newJb.getString("skuId");
						
						//get decode value,else use skuid to display
						Optional<TaMasterCd> rlc = tmc.findById(skuId);
						if(rlc.isPresent()) {
							TaMasterCd skuenti = rlc.get();
							ou.getAssignedLicenses().add(skuenti.getDecode());
						}
						else {
							ou.getAssignedLicenses().add(skuId);
						}
					}
				}
				ou.setAccountEnabled(accountEnabled);
				ou.setUsageLocation(usageLocation);
				ou.setId(uid);
				ou.setUserPrincipalName(userPrincipalName);
				ou.setDisplayName(displayName);
				ll.add(ou);
			}
		}
		else {
			getNextUrl(nextPage, accessToken, times, ll);
		}
	}
	
}
