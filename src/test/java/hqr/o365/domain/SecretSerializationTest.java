package hqr.o365.domain;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretSerializationTest {
    @Test
    void administrativeResponsesDoNotExposeStoredSecrets() {
        TaOfficeInfo office = new TaOfficeInfo();
        office.setPasswd("private-password");
        office.setSecretId("private-secret");
        String officeJson = JSON.toJSONString(office);
        assertFalse(officeJson.contains("private-password"));
        assertFalse(officeJson.contains("private-secret"));

        TaAppRpt report = new TaAppRpt();
        report.setSecretId("report-secret");
        assertFalse(JSON.toJSONString(report).contains("report-secret"));

        TaInviteInfo invite = new TaInviteInfo();
        invite.setResult("user@example.com|temporary-password");
        assertFalse(JSON.toJSONString(invite).contains("temporary-password"));
    }
}
