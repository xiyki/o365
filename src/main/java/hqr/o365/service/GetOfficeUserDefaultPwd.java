package hqr.o365.service;

import java.util.Optional;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import hqr.o365.dao.TaMasterCdRepo;
import hqr.o365.domain.TaMasterCd;

@Service
public class GetOfficeUserDefaultPwd {

	@Autowired
	private TaMasterCdRepo tmc;
	
	@Cacheable(value="cacheDefaultPwd")
	public String getDefaultPwd() {
		Optional<TaMasterCd> opt = tmc.findById("DEFAULT_PASSWORD");
		if(opt.isPresent()) {
			TaMasterCd setting = opt.get();
			String pwd = setting.getCd();
			if (pwd != null && !pwd.isEmpty() && !"Mjj@1234".equals(pwd)) {
				return pwd;
			}
			pwd = generatePassword();
			setting.setCd(pwd);
			tmc.saveAndFlush(setting);
			return pwd;
		}
		return generatePassword();
	}

	private String generatePassword() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
		SecureRandom random = new SecureRandom();
		StringBuilder password = new StringBuilder("Aa1!");
		for (int i = 0; i < 16; i++) {
			password.append(chars.charAt(random.nextInt(chars.length())));
		}
		return password.toString();
	}
	
}
