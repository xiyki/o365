package hqr.o365.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.util.URLUtil;

@Service
public class ValidateAppInfo {
	
	private RestTemplate restTemplate = GraphHttpClient.create();
	
	@Value("${UA}")
    private String ua;

	public boolean checkAndGet(String tenantId, String appId, String secretId) {
		return !getToken(tenantId, appId, secretId).isEmpty();
	}

	public String getToken(String tenantId, String appId, String secretId) {
		if (tenantId == null || !tenantId.matches("[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*")
				|| appId == null || secretId == null) {
			return "";
		}
		String endpoint = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.USER_AGENT, ua);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String json = "client_id="+URLUtil.encodeQuery(appId)+"&client_secret="+URLUtil.encodeQuery(secretId)+"&grant_type=client_credentials&scope=https://graph.microsoft.com/.default";
		HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);

		try{
			ResponseEntity<String> response= restTemplate.postForEntity(endpoint, requestEntity, String.class);
			if(response.getStatusCodeValue()==200) {
				JSONObject jo = JSON.parseObject(response.getBody());
				String token = jo.getString("access_token");
				return token == null ? "" : token;
			}
			else {
				return "";
			}
		} catch (Exception e) {
			return "";
		}
	}
	
}
