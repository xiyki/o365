package hqr.o365.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import hqr.o365.dao.TaOfficeInfoRepo;
import hqr.o365.domain.TaOfficeInfo;

@Service
public class SaveOfficeInfo {
	@Autowired
	private TaOfficeInfoRepo repo;
	
	@CacheEvict(value="cacheOfficeInfo", allEntries = true)
	public boolean save(TaOfficeInfo ti) {
		try {
			if (ti.getSeqNo() != 0) {
				TaOfficeInfo existing = repo.findById(ti.getSeqNo()).orElse(null);
				if (existing == null) {
					return false;
				}
				if (ti.getPasswd() == null || ti.getPasswd().isEmpty()) {
					ti.setPasswd(existing.getPasswd());
				}
				if (ti.getSecretId() == null || ti.getSecretId().isEmpty()) {
					ti.setSecretId(existing.getSecretId());
				}
				ti.setSelected(existing.getSelected());
				ti.setCreateDt(existing.getCreateDt());
			} else if (ti.getSecretId() == null || ti.getSecretId().isEmpty()) {
				return false;
			} else {
				ti.setSelected("否");
			}
			repo.save(ti);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
}
