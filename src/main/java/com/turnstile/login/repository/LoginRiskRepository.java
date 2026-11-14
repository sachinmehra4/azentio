package com.turnstile.login.repository;

import com.turnstile.login.api.RiskDecisionResponse;
import com.turnstile.login.api.LoginRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class LoginRiskRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<RiskDecisionResponse> decisionMapper = (rs, rowNum) ->
        new RiskDecisionResponse(
            rs.getLong("id"),
            rs.getString("event_id"),
            rs.getLong("customer_id"),
            rs.getInt("score"),
            rs.getString("decision"),
            split(rs.getString("reason_codes")),
            rs.getString("step_up_status"),
            rs.getTimestamp("created_at").toInstant()
        );

    public LoginRiskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RiskDecisionResponse> findDecision(String eventId) {
        return jdbc.query("SELECT * FROM risk_decision WHERE event_id = ?",
                decisionMapper, eventId).stream().findFirst();
    }

    public Optional<RiskDecisionResponse> findDecisionById(long id) {
        return jdbc.query("SELECT * FROM risk_decision WHERE id = ?",
                decisionMapper, id).stream().findFirst();
    }

    public boolean customerExists(long customerId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer WHERE id = ?", Integer.class, customerId);
        return count != null && count > 0;
    }

    public void createCustomer(long id, String name) {
        jdbc.update("INSERT INTO customer(id, full_name) VALUES (?, ?)", id, name);
    }

    public boolean isTrusted(long customerId, String deviceId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM trusted_device
            WHERE customer_id = ? AND device_id = ?
            """, Integer.class, customerId, deviceId);
        return count != null && count > 0;
    }

    public boolean hasPassedCountry(long customerId, String country,
                                    Instant fromInclusive, Instant toExclusive) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM login_attempt
            WHERE customer_id = ? AND outcome = 'PASSED' AND country_code = ?
              AND event_time >= ? AND event_time < ?
            """, Integer.class, customerId, country,
                Timestamp.from(fromInclusive), Timestamp.from(toExclusive));
        return count != null && count > 0;
    }

    public PreviousLogin findPreviousPassed(long customerId, Instant before) {
        return jdbc.query("""
            SELECT latitude, longitude, event_time
            FROM login_attempt
            WHERE customer_id = ? AND outcome = 'PASSED' AND event_time < ?
            ORDER BY event_time DESC
            LIMIT 1
            """, rs -> rs.next()
                ? new PreviousLogin(rs.getDouble("latitude"), rs.getDouble("longitude"),
                    rs.getTimestamp("event_time").toInstant())
                : null,
            customerId, Timestamp.from(before));
    }

    public int countFailedInWindow(long customerId, Instant fromInclusive, Instant toExclusive) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM login_attempt
            WHERE customer_id = ? AND outcome = 'FAILED'
              AND event_time >= ? AND event_time < ?
            """, Integer.class, customerId,
                Timestamp.from(fromInclusive), Timestamp.from(toExclusive));
        return count == null ? 0 : count;
    }

    public void saveAttempt(LoginRequest request, String outcome) {
        jdbc.update("""
            INSERT INTO login_attempt(event_id, customer_id, device_id, country_code,
              latitude, longitude, credential_valid, outcome, event_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            request.eventId(), request.customerId(), request.deviceId(),
            request.countryCode().toUpperCase(), request.latitude(), request.longitude(),
            request.credentialValid(), outcome, Timestamp.from(request.eventTime()));
    }

    public RiskDecisionResponse saveDecision(String eventId, long customerId, int score,
            String decision, List<String> reasons, String stepUpStatus, Instant createdAt) {
        jdbc.update("""
            INSERT INTO risk_decision(event_id, customer_id, score, decision,
              reason_codes, step_up_status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, eventId, customerId, score, decision, String.join(",", reasons),
            stepUpStatus, Timestamp.from(createdAt));
        return findDecision(eventId).orElseThrow();
    }

    public void trustDevice(long customerId, String deviceId, Instant at) {
        Integer exists = jdbc.queryForObject("""
            SELECT COUNT(*) FROM trusted_device WHERE customer_id = ? AND device_id = ?
            """, Integer.class, customerId, deviceId);
        if (exists != null && exists > 0) {
            jdbc.update("""
                UPDATE trusted_device SET trusted_at = ?
                WHERE customer_id = ? AND device_id = ?
                """, Timestamp.from(at), customerId, deviceId);
        } else {
            jdbc.update("""
                INSERT INTO trusted_device(customer_id, device_id, trusted_at)
                VALUES (?, ?, ?)
                """, customerId, deviceId, Timestamp.from(at));
        }
    }

    public void trustDeviceForEvent(String eventId, Instant at) {
        jdbc.update("""
            UPDATE trusted_device
            SET trusted_at = ?
            WHERE (customer_id, device_id) IN
              (SELECT customer_id, device_id FROM login_attempt WHERE event_id = ?)
            """, Timestamp.from(at), eventId);

        Integer exists = jdbc.queryForObject("""
            SELECT COUNT(*) FROM trusted_device td
            JOIN login_attempt la ON la.customer_id = td.customer_id
                                  AND la.device_id = td.device_id
            WHERE la.event_id = ?
            """, Integer.class, eventId);
        if (exists == null || exists == 0) {
            jdbc.update("""
                INSERT INTO trusted_device(customer_id, device_id, trusted_at)
                SELECT customer_id, device_id, ?
                FROM login_attempt WHERE event_id = ?
                """, Timestamp.from(at), eventId);
        }
    }

    public void updateStepUp(long id, String status) {
        jdbc.update("UPDATE risk_decision SET step_up_status = ? WHERE id = ?", status, id);
    }

    public List<RiskDecisionResponse> listDecisions(int limit, int offset) {
        return jdbc.query("""
            SELECT * FROM risk_decision
            ORDER BY created_at DESC, id DESC
            LIMIT ? OFFSET ?
            """, decisionMapper, limit, offset);
    }

    public long countDecisions() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM risk_decision", Long.class);
        return count == null ? 0 : count;
    }

    public int countAttempts() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM login_attempt", Integer.class);
        return count == null ? 0 : count;
    }

    private static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split(","));
    }

    public record PreviousLogin(double latitude, double longitude, Instant eventTime) {}
}
