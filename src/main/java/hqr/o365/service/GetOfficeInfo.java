package hqr.o365.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import hqr.o365.dao.TaOfficeInfoRepo;
import hqr.o365.domain.TaOfficeInfo;

@Service
public class GetOfficeInfo {
	
	@Autowired
	private TaOfficeInfoRepo repo;

	@Autowired
	private SelectedOfficeInfo selectedOfficeInfo;
	
	@Value("${UA}")
    private String ua;

	public String getAllOfficeInfo(int intRows, int intPage) {
		long total = repo.count();
		
		Page<TaOfficeInfo> pages = repo.findAll(PageRequest.of(intPage - 1, intRows));
		
		Integer selectedId = selectedOfficeInfo.currentId();
		List<JSONObject> rows = new ArrayList<>();
		for (TaOfficeInfo info : pages.getContent()) {
			JSONObject visible = (JSONObject) JSON.toJSON(info);
			visible.put("selected", selectedId != null && selectedId == info.getSeqNo() ? "是" : "否");
			rows.add(visible);
		}
		HashMap map = new HashMap();
		map.put("total", total);
		map.put("rows", rows);
		
		return JSON.toJSON(map).toString();
	}
	
}
