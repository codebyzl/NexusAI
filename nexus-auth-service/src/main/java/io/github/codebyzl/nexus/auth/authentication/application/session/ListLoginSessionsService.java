package io.github.codebyzl.nexus.auth.authentication.application.session;

import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListLoginSessionsService {

    private final LoginSessionRepository loginSessionRepository;

    @Transactional(readOnly = true)
    public List<LoginSessionResult> getListSessions(ListLoginSessionsQuery query) {
        return loginSessionRepository
                .findByAccountId(query.accountId())
                .stream()
                .map(session -> new LoginSessionResult(
                        session.getId(),
                        session.getStatus(),
                        session.getId().equals(query.currentSessionId()),
                        session.getCreatedAt(),
                        session.getExpiresAt(),
                        session.getRevokedAt()
                ))
                .toList();
    }
}