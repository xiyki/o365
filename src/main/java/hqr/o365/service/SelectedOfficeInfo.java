package hqr.o365.service;

import hqr.o365.dao.TaOfficeInfoRepo;
import hqr.o365.domain.TaOfficeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

@Service
public class SelectedOfficeInfo {
    public static final String SESSION_KEY = "selectedOfficeSeqNo";

    @Autowired
    private TaOfficeInfoRepo repo;

    public List<TaOfficeInfo> current() {
        Integer selectedId = sessionSelection();
        if (selectedId != null) {
            return repo.findById(selectedId).map(Collections::singletonList)
                    .orElseGet(Collections::emptyList);
        }
        return repo.findBySelected("是");
    }

    public Integer currentId() {
        Integer selectedId = sessionSelection();
        if (selectedId != null) {
            return selectedId;
        }
        List<TaOfficeInfo> selected = repo.findBySelected("是");
        return selected.isEmpty() ? null : selected.get(0).getSeqNo();
    }

    private Integer sessionSelection() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpSession session = ((ServletRequestAttributes) attributes).getRequest().getSession(false);
        if (session == null) {
            return null;
        }
        Object selected = session.getAttribute(SESSION_KEY);
        return selected instanceof Integer ? (Integer) selected : null;
    }
}
