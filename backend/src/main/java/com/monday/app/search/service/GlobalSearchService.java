package com.monday.app.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.search.dto.ModuleSearchResponse;
import com.monday.app.search.dto.SearchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Service
public class GlobalSearchService {

    private static final Logger log = LoggerFactory.getLogger(GlobalSearchService.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public GlobalSearchService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * Search an individual module by its ID
     */
    public ModuleSearchResponse searchModule(String moduleId, String query) {
        String cleanQuery = (query != null) ? query.trim() : "";
        if (cleanQuery.isEmpty()) {
            return new ModuleSearchResponse(moduleId, getModuleLabel(moduleId), query, 0, 0, Collections.emptyList());
        }

        long start = System.currentTimeMillis();
        List<SearchResultDto> results = new ArrayList<>();

        switch (moduleId.toLowerCase()) {
            case "projects" -> results.addAll(searchProjects(cleanQuery));
            case "events" -> results.addAll(searchEvents(cleanQuery));
            case "finance" -> results.addAll(searchFinance(cleanQuery));
            case "vehicles" -> results.addAll(searchVehicles(cleanQuery));
            case "administration" -> results.addAll(searchAdministration(cleanQuery));
            case "career" -> results.addAll(searchCareer(cleanQuery));
            case "intelligence" -> results.addAll(searchIntelligence(cleanQuery));
            default -> log.warn("Unknown search module requested: {}", moduleId);
        }

        long duration = System.currentTimeMillis() - start;
        return new ModuleSearchResponse(
                moduleId.toLowerCase(),
                getModuleLabel(moduleId),
                cleanQuery,
                results.size(),
                duration,
                results
        );
    }

    /**
     * Search across all 7 modules
     */
    public Map<String, ModuleSearchResponse> searchAll(String query) {
        String[] modules = {"projects", "events", "finance", "vehicles", "administration", "career", "intelligence"};
        Map<String, ModuleSearchResponse> map = new LinkedHashMap<>();
        for (String mod : modules) {
            map.put(mod, searchModule(mod, query));
        }
        return map;
    }

    // =========================================================================
    // 1. PROJECTS MODULE SEARCH
    // =========================================================================
    public List<SearchResultDto> searchProjects(String query) {
        List<SearchResultDto> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase() + "%";
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", pattern);

        // A. Projects
        try {
            String sqlProjects = """
                SELECT id, name, description, category, status, priority, due_at
                FROM projects
                WHERE LOWER(name) LIKE :pattern 
                   OR LOWER(COALESCE(description, '')) LIKE :pattern 
                   OR LOWER(COALESCE(category, '')) LIKE :pattern
                ORDER BY created_at DESC LIMIT 15
            """;
            list.addAll(jdbc.query(sqlProjects, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("projects");
                item.setModuleLabel("Projects");
                item.setCategory("Project");
                item.setTitle(rs.getString("name"));
                item.setSubtitle(rs.getString("category") + " · " + rs.getString("status"));
                item.setSnippet(rs.getString("description"));
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getString("priority"));
                item.setIcon("account_tree");
                item.setDate(formatTimestamp(rs.getTimestamp("due_at")));
                item.addMeta("type", "project");
                return item;
            }));
        } catch (Exception e) {
            log.error("Projects search error: {}", e.getMessage());
        }

        // B. Milestones
        try {
            String sqlMilestones = """
                SELECT m.id, m.name, m.description, m.status, m.priority, m.due_at, p.name AS project_name, p.id AS project_id
                FROM milestones m
                LEFT JOIN projects p ON m.project_id = p.id
                WHERE LOWER(m.name) LIKE :pattern 
                   OR LOWER(COALESCE(m.description, '')) LIKE :pattern
                ORDER BY m.created_at DESC LIMIT 15
            """;
            list.addAll(jdbc.query(sqlMilestones, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("projects");
                item.setModuleLabel("Projects");
                item.setCategory("Milestone");
                item.setTitle(rs.getString("name"));
                String pName = rs.getString("project_name");
                item.setSubtitle((pName != null ? pName : "Project") + " · " + rs.getString("status"));
                item.setSnippet(rs.getString("description"));
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getString("priority"));
                item.setIcon("flag");
                item.setDate(formatTimestamp(rs.getTimestamp("due_at")));
                item.addMeta("type", "milestone");
                item.addMeta("projectId", rs.getString("project_id"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Milestones search error: {}", e.getMessage());
        }

        // C. Tasks
        try {
            String sqlTasks = """
                SELECT t.id, t.title, t.description, t.status, t.priority, t.due_at, p.name AS project_name, p.id AS project_id
                FROM tasks t
                LEFT JOIN projects p ON t.project_id = p.id
                WHERE LOWER(t.title) LIKE :pattern 
                   OR LOWER(COALESCE(t.description, '')) LIKE :pattern
                ORDER BY t.created_at DESC LIMIT 20
            """;
            list.addAll(jdbc.query(sqlTasks, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("projects");
                item.setModuleLabel("Projects");
                item.setCategory("Task");
                item.setTitle(rs.getString("title"));
                String pName = rs.getString("project_name");
                item.setSubtitle((pName != null ? pName : "Task") + " · " + rs.getString("status"));
                item.setSnippet(rs.getString("description"));
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getString("priority"));
                item.setIcon("check_circle");
                item.setDate(formatTimestamp(rs.getTimestamp("due_at")));
                item.addMeta("type", "task");
                item.addMeta("projectId", rs.getString("project_id"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Tasks search error: {}", e.getMessage());
        }

        return list;
    }

    // =========================================================================
    // 2. EVENTS MODULE SEARCH
    // =========================================================================
    public List<SearchResultDto> searchEvents(String query) {
        List<SearchResultDto> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase() + "%";
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", pattern);

        // A. Events
        try {
            String sql = """
                SELECT id, title, description, event_type, start_at, end_at, location, status, requires_prep, prep_title
                FROM events
                WHERE LOWER(title) LIKE :pattern
                   OR LOWER(COALESCE(description, '')) LIKE :pattern
                   OR LOWER(COALESCE(location, '')) LIKE :pattern
                   OR LOWER(COALESCE(prep_title, '')) LIKE :pattern
                ORDER BY start_at DESC LIMIT 20
            """;
            list.addAll(jdbc.query(sql, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("events");
                item.setModuleLabel("Events");
                item.setCategory("Event");
                item.setTitle(rs.getString("title"));
                String loc = rs.getString("location");
                item.setSubtitle(rs.getString("event_type") + (loc != null && !loc.isBlank() ? " · " + loc : ""));
                item.setSnippet(rs.getString("description"));
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getBoolean("requires_prep") ? "Prep Needed" : null);
                item.setIcon("calendar_month");
                item.setDate(formatTimestamp(rs.getTimestamp("start_at")));
                item.addMeta("location", loc);
                return item;
            }));
        } catch (Exception e) {
            log.error("Events search error: {}", e.getMessage());
        }

        // B. Event Prep Items
        try {
            String sqlPrep = """
                SELECT p.id, p.text, p.completed, p.due, e.id AS event_id, e.title AS event_title
                FROM event_prep_items p
                JOIN events e ON p.event_id = e.id
                WHERE LOWER(p.text) LIKE :pattern
                ORDER BY p.due ASC LIMIT 10
            """;
            list.addAll(jdbc.query(sqlPrep, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("events");
                item.setModuleLabel("Events");
                item.setCategory("Prep Checklist");
                item.setTitle(rs.getString("text"));
                item.setSubtitle("For: " + rs.getString("event_title"));
                item.setSnippet("Event preparation commitment");
                item.setStatus(rs.getBoolean("completed") ? "COMPLETED" : "PENDING");
                item.setBadge(rs.getBoolean("completed") ? "Done" : "Pending");
                item.setIcon("playlist_add_check");
                item.setDate(rs.getString("due"));
                item.addMeta("eventId", rs.getString("event_id"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Event prep items search error: {}", e.getMessage());
        }

        return list;
    }

    // =========================================================================
    // 3. FINANCE MODULE SEARCH
    // =========================================================================
    public List<SearchResultDto> searchFinance(String query) {
        List<SearchResultDto> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase() + "%";
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", pattern);

        // A. Transactions
        try {
            String sqlTx = """
                SELECT t.id, t.transaction_type, t.amount, t.transaction_at, t.payment_method, t.description, t.notes, c.name AS category_name
                FROM transactions t
                LEFT JOIN budget_categories c ON t.category_id = c.id
                WHERE LOWER(t.description) LIKE :pattern
                   OR LOWER(COALESCE(t.notes, '')) LIKE :pattern
                   OR LOWER(COALESCE(t.payment_method, '')) LIKE :pattern
                   OR LOWER(COALESCE(c.name, '')) LIKE :pattern
                ORDER BY t.transaction_at DESC LIMIT 20
            """;
            list.addAll(jdbc.query(sqlTx, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("finance");
                item.setModuleLabel("Finance");
                item.setCategory("Transaction");
                item.setTitle(rs.getString("description"));
                String cat = rs.getString("category_name");
                double amt = rs.getDouble("amount");
                String sign = "EXPENSE".equalsIgnoreCase(rs.getString("transaction_type")) ? "-₹" : "+₹";
                item.setSubtitle(String.format("%s%.2f · %s", sign, amt, cat != null ? cat : "General"));
                item.setSnippet(rs.getString("notes"));
                item.setStatus(rs.getString("transaction_type"));
                item.setBadge(rs.getString("payment_method"));
                item.setIcon("payments");
                item.setDate(formatTimestamp(rs.getTimestamp("transaction_at")));
                item.addMeta("amount", amt);
                return item;
            }));
        } catch (Exception e) {
            log.error("Transactions search error: {}", e.getMessage());
        }

        // B. Budgets
        try {
            String sqlBudgets = """
                SELECT b.id, b.name, b.amount, c.name AS category_name
                FROM budgets b
                LEFT JOIN budget_categories c ON b.category_id = c.id
                WHERE LOWER(b.name) LIKE :pattern
                   OR LOWER(COALESCE(c.name, '')) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlBudgets, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("finance");
                item.setModuleLabel("Finance");
                item.setCategory("Budget");
                item.setTitle(rs.getString("name"));
                item.setSubtitle(String.format("Limit: ₹%.2f · Category: %s", rs.getDouble("amount"), rs.getString("category_name")));
                item.setSnippet("Operating budget threshold");
                item.setStatus("ACTIVE");
                item.setBadge(String.format("₹%.0f", rs.getDouble("amount")));
                item.setIcon("account_balance_wallet");
                item.addMeta("amount", rs.getDouble("amount"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Budgets search error: {}", e.getMessage());
        }

        // C. Accounts
        try {
            String sqlAccounts = """
                SELECT id, institution, account_type, balance, currency
                FROM finance_accounts
                WHERE LOWER(institution) LIKE :pattern
                   OR LOWER(account_type) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlAccounts, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("finance");
                item.setModuleLabel("Finance");
                item.setCategory("Account");
                item.setTitle(rs.getString("institution"));
                item.setSubtitle(rs.getString("account_type") + " · Balance: " + rs.getString("currency") + " " + String.format("%.2f", rs.getDouble("balance")));
                item.setSnippet("Treasury liquidity account");
                item.setStatus("RECONCILED");
                item.setBadge(rs.getString("currency"));
                item.setIcon("account_balance");
                item.addMeta("balance", rs.getDouble("balance"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Finance accounts search error: {}", e.getMessage());
        }

        // D. Commitments
        try {
            String sqlCommitments = """
                SELECT id, name, due_date, amount, status, is_urgent
                FROM finance_commitments
                WHERE LOWER(name) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlCommitments, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("finance");
                item.setModuleLabel("Finance");
                item.setCategory("Financial Commitment");
                item.setTitle(rs.getString("name"));
                item.setSubtitle(String.format("Due: %s · Amount: ₹%.2f", rs.getString("due_date"), rs.getDouble("amount")));
                item.setSnippet("Impending financial outflow");
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getBoolean("is_urgent") ? "URGENT" : "Upcoming");
                item.setIcon("receipt_long");
                item.setDate(rs.getString("due_date"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Finance commitments search error: {}", e.getMessage());
        }

        return list;
    }

    // =========================================================================
    // 4. VEHICLES MODULE SEARCH
    // =========================================================================
    public List<SearchResultDto> searchVehicles(String query) {
        List<SearchResultDto> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase() + "%";
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", pattern);

        // A. Vehicles
        try {
            String sqlVehicles = """
                SELECT id, name, edition, variant, registration_number, type, current_km, status
                FROM vehicles
                WHERE LOWER(name) LIKE :pattern
                   OR LOWER(COALESCE(edition, '')) LIKE :pattern
                   OR LOWER(COALESCE(registration_number, '')) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlVehicles, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("vehicles");
                item.setModuleLabel("Vehicles");
                item.setCategory("Vehicle Fleet");
                item.setTitle(rs.getString("name"));
                String reg = rs.getString("registration_number");
                int km = rs.getInt("current_km");
                item.setSubtitle(String.format("%s · %s · %d km", reg != null ? reg : "", rs.getString("type"), km));
                item.setSnippet(rs.getString("edition") != null ? rs.getString("edition") : "Primary machine");
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getString("type"));
                item.setIcon("two_wheeler");
                item.addMeta("vehicleId", rs.getString("id"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Vehicles search error: {}", e.getMessage());
        }

        // B. Vehicle Documents
        try {
            String sqlDocs = """
                SELECT d.id, d.title, d.doc_type, d.identifier, d.status, d.expiry_date, v.id AS vehicle_id, v.name AS vehicle_name
                FROM vehicle_documents d
                LEFT JOIN vehicles v ON d.vehicle_id = v.id
                WHERE LOWER(d.title) LIKE :pattern
                   OR LOWER(COALESCE(d.identifier, '')) LIKE :pattern
                   OR LOWER(COALESCE(d.doc_type, '')) LIKE :pattern
                LIMIT 15
            """;
            list.addAll(jdbc.query(sqlDocs, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("vehicles");
                item.setModuleLabel("Vehicles");
                item.setCategory("Vehicle Document");
                item.setTitle(rs.getString("title"));
                String vName = rs.getString("vehicle_name");
                item.setSubtitle(String.format("%s · %s", vName != null ? vName : "Vehicle", rs.getString("doc_type")));
                item.setSnippet("Ident: " + rs.getString("identifier"));
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getString("doc_type"));
                item.setIcon("description");
                item.setDate(rs.getString("expiry_date"));
                item.addMeta("vehicleId", rs.getString("vehicle_id"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Vehicle documents search error: {}", e.getMessage());
        }

        // C. Service Records
        try {
            String sqlServices = """
                SELECT s.id, s.title, s.service_center, s.work_performed, s.odometer_km, s.service_date, v.id AS vehicle_id, v.name AS vehicle_name
                FROM vehicle_service_records s
                LEFT JOIN vehicles v ON s.vehicle_id = v.id
                WHERE LOWER(s.title) LIKE :pattern
                   OR LOWER(COALESCE(s.service_center, '')) LIKE :pattern
                   OR LOWER(COALESCE(s.work_performed, '')) LIKE :pattern
                LIMIT 15
            """;
            list.addAll(jdbc.query(sqlServices, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("vehicles");
                item.setModuleLabel("Vehicles");
                item.setCategory("Service Record");
                item.setTitle(rs.getString("title"));
                String sc = rs.getString("service_center");
                item.setSubtitle(String.format("%s%s · %d km", rs.getString("vehicle_name") != null ? rs.getString("vehicle_name") : "Fleet", sc != null ? " · " + sc : "", rs.getInt("odometer_km")));
                item.setSnippet(rs.getString("work_performed"));
                item.setStatus("COMPLETED");
                item.setBadge("Service");
                item.setIcon("build");
                item.setDate(rs.getString("service_date"));
                item.addMeta("vehicleId", rs.getString("vehicle_id"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Vehicle service search error: {}", e.getMessage());
        }

        // D. Fuel Logs
        try {
            String sqlFuel = """
                SELECT f.id, f.brand, f.station_location, f.total_cost, f.logged_at, v.id AS vehicle_id, v.name AS vehicle_name
                FROM vehicle_fuel_logs f
                LEFT JOIN vehicles v ON f.vehicle_id = v.id
                WHERE LOWER(COALESCE(f.brand, '')) LIKE :pattern
                   OR LOWER(COALESCE(f.station_location, '')) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlFuel, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("vehicles");
                item.setModuleLabel("Vehicles");
                item.setCategory("Fuel Record");
                item.setTitle("Fuel: " + (rs.getString("brand") != null ? rs.getString("brand") : "Fillup"));
                item.setSubtitle(String.format("₹%.2f · %s", rs.getDouble("total_cost"), rs.getString("station_location") != null ? rs.getString("station_location") : "Station"));
                item.setSnippet("Telemetry fuel transaction");
                item.setStatus("LOGGED");
                item.setBadge("Fuel");
                item.setIcon("local_gas_station");
                item.setDate(formatTimestamp(rs.getTimestamp("logged_at")));
                item.addMeta("vehicleId", rs.getString("vehicle_id"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Vehicle fuel search error: {}", e.getMessage());
        }

        return list;
    }

    // =========================================================================
    // 5. ADMINISTRATION MODULE SEARCH
    // =========================================================================
    public List<SearchResultDto> searchAdministration(String query) {
        List<SearchResultDto> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase() + "%";
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", pattern);

        // A. Obligations
        try {
            String sqlObligations = """
                SELECT id, name, description, provider_name, category, expected_amount, currency, next_due_date, status, obligation_type
                FROM administration_obligations
                WHERE archived_at IS NULL
                  AND (LOWER(name) LIKE :pattern 
                    OR LOWER(COALESCE(provider_name, '')) LIKE :pattern 
                    OR LOWER(COALESCE(description, '')) LIKE :pattern 
                    OR LOWER(category) LIKE :pattern)
                ORDER BY next_due_date ASC LIMIT 15
            """;
            list.addAll(jdbc.query(sqlObligations, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("administration");
                item.setModuleLabel("Administration");
                item.setCategory("Obligation / Bill");
                item.setTitle(rs.getString("name"));
                String prov = rs.getString("provider_name");
                double amt = rs.getDouble("expected_amount");
                item.setSubtitle(String.format("%s%s · %s %.2f", prov != null && !prov.isBlank() ? prov + " · " : "", rs.getString("category"), rs.getString("currency"), amt));
                item.setSnippet(rs.getString("description"));
                item.setStatus(rs.getString("status"));
                item.setBadge(rs.getString("obligation_type"));
                item.setIcon("receipt_long");
                item.setDate(rs.getString("next_due_date"));
                item.addMeta("subTab", "obligation");
                return item;
            }));
        } catch (Exception e) {
            log.error("Administration obligations search error: {}", e.getMessage());
        }

        // B. People
        try {
            String sqlPeople = """
                SELECT id, display_name, relationship_label, birthday, contact_information, notes
                FROM administration_people
                WHERE archived_at IS NULL
                  AND (LOWER(display_name) LIKE :pattern 
                    OR LOWER(COALESCE(relationship_label, '')) LIKE :pattern 
                    OR LOWER(COALESCE(contact_information, '')) LIKE :pattern 
                    OR LOWER(COALESCE(notes, '')) LIKE :pattern)
                LIMIT 15
            """;
            list.addAll(jdbc.query(sqlPeople, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("administration");
                item.setModuleLabel("Administration");
                item.setCategory("Person");
                item.setTitle(rs.getString("display_name"));
                item.setSubtitle(rs.getString("relationship_label") != null ? rs.getString("relationship_label") : "Contact");
                item.setSnippet(rs.getString("contact_information") != null ? rs.getString("contact_information") : rs.getString("notes"));
                item.setStatus("ACTIVE");
                item.setBadge("Contact");
                item.setIcon("person");
                item.setDate(rs.getString("birthday"));
                item.addMeta("subTab", "person");
                return item;
            }));
        } catch (Exception e) {
            log.error("Administration people search error: {}", e.getMessage());
        }

        // C. Gadgets
        try {
            String sqlGadgets = """
                SELECT id, name, category, brand, model, serial_number
                FROM administration_gadgets
                WHERE archived_at IS NULL
                  AND (LOWER(name) LIKE :pattern 
                    OR LOWER(COALESCE(brand, '')) LIKE :pattern 
                    OR LOWER(COALESCE(model, '')) LIKE :pattern 
                    OR LOWER(COALESCE(serial_number, '')) LIKE :pattern)
                LIMIT 15
            """;
            list.addAll(jdbc.query(sqlGadgets, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("administration");
                item.setModuleLabel("Administration");
                item.setCategory("Gadget");
                item.setTitle(rs.getString("name"));
                String b = rs.getString("brand");
                String m = rs.getString("model");
                item.setSubtitle((b != null ? b : "") + (m != null ? " " + m : "") + " · " + rs.getString("category"));
                item.setSnippet("S/N: " + (rs.getString("serial_number") != null ? rs.getString("serial_number") : "N/A"));
                item.setStatus("ACTIVE");
                item.setBadge(rs.getString("category"));
                item.setIcon("devices");
                item.addMeta("subTab", "gadget");
                return item;
            }));
        } catch (Exception e) {
            log.error("Administration gadgets search error: {}", e.getMessage());
        }

        // D. Notes
        try {
            String sqlNotes = """
                SELECT id, title, body, updated_at
                FROM administration_notes
                WHERE LOWER(title) LIKE :pattern 
                   OR LOWER(COALESCE(body, '')) LIKE :pattern
                ORDER BY updated_at DESC LIMIT 15
            """;
            list.addAll(jdbc.query(sqlNotes, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("administration");
                item.setModuleLabel("Administration");
                item.setCategory("Note");
                item.setTitle(rs.getString("title"));
                item.setSubtitle("Executive Notepad");
                item.setSnippet(rs.getString("body"));
                item.setStatus("SAVED");
                item.setBadge("Note");
                item.setIcon("edit_note");
                item.setDate(formatTimestamp(rs.getTimestamp("updated_at")));
                item.addMeta("subTab", "note");
                return item;
            }));
        } catch (Exception e) {
            log.error("Administration notes search error: {}", e.getMessage());
        }

        // E. Important Dates
        try {
            String sqlDates = """
                SELECT id, title, date_type, date_value, notes
                FROM administration_important_dates
                WHERE LOWER(title) LIKE :pattern 
                   OR LOWER(COALESCE(notes, '')) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlDates, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("administration");
                item.setModuleLabel("Administration");
                item.setCategory("Important Date");
                item.setTitle(rs.getString("title"));
                item.setSubtitle(rs.getString("date_type"));
                item.setSnippet(rs.getString("notes"));
                item.setStatus("ACTIVE");
                item.setBadge(rs.getString("date_type"));
                item.setIcon("event");
                item.setDate(rs.getString("date_value"));
                item.addMeta("subTab", "person");
                return item;
            }));
        } catch (Exception e) {
            log.error("Administration dates search error: {}", e.getMessage());
        }

        return list;
    }

    // =========================================================================
    // 6. CAREER MODULE SEARCH
    // =========================================================================
    public List<SearchResultDto> searchCareer(String query) {
        List<SearchResultDto> list = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        try {
            String sql = "SELECT id, name, description, color, plan_data FROM career_plans LIMIT 5";
            jdbc.query(sql, (rs) -> {
                String planId = rs.getString("id");
                String planName = rs.getString("name");
                String planDesc = rs.getString("description");
                String planDataJson = rs.getString("plan_data");

                // Check plan itself
                if ((planName != null && planName.toLowerCase().contains(lowerQuery)) ||
                    (planDesc != null && planDesc.toLowerCase().contains(lowerQuery))) {
                    SearchResultDto planItem = new SearchResultDto();
                    planItem.setId(planId);
                    planItem.setModuleId("career");
                    planItem.setModuleLabel("Career");
                    planItem.setCategory("Career Trajectory");
                    planItem.setTitle(planName != null ? planName : "Career Roadmap");
                    planItem.setSubtitle("Strategic Transition Architecture");
                    planItem.setSnippet(planDesc);
                    planItem.setStatus("ACTIVE");
                    planItem.setBadge("Plan");
                    planItem.setIcon("school");
                    list.add(planItem);
                }

                // Parse nested milestones & tasks in plan_data JSON
                if (planDataJson != null && !planDataJson.isBlank()) {
                    try {
                        JsonNode root = objectMapper.readTree(planDataJson);
                        JsonNode milestones = root.path("milestones");
                        if (milestones.isArray()) {
                            for (JsonNode m : milestones) {
                                String mId = m.path("id").asText();
                                String mName = m.path("name").asText();
                                String mDesc = m.path("description").asText("");
                                String mDue = m.path("dueDate").asText("");

                                boolean mMatches = (mName != null && mName.toLowerCase().contains(lowerQuery)) ||
                                                   (mDesc != null && mDesc.toLowerCase().contains(lowerQuery));

                                if (mMatches) {
                                    SearchResultDto mItem = new SearchResultDto();
                                    mItem.setId(mId);
                                    mItem.setModuleId("career");
                                    mItem.setModuleLabel("Career");
                                    mItem.setCategory("Career Milestone");
                                    mItem.setTitle(mName);
                                    mItem.setSubtitle("Roadmap Pillar Milestone");
                                    mItem.setSnippet(mDesc);
                                    mItem.setStatus(m.path("isOngoing").asBoolean() ? "ONGOING" : "ACTIVE");
                                    mItem.setBadge("Milestone");
                                    mItem.setIcon("flag");
                                    mItem.setDate(mDue);
                                    list.add(mItem);
                                }

                                // Search tasks inside milestone
                                JsonNode tasks = m.path("tasks");
                                if (tasks.isArray()) {
                                    for (JsonNode t : tasks) {
                                        searchCareerTaskNode(t, mName, lowerQuery, list);
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Failed parsing career plan JSON: {}", ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Career search error: {}", e.getMessage());
        }

        return list;
    }

    private void searchCareerTaskNode(JsonNode t, String milestoneName, String query, List<SearchResultDto> list) {
        String tId = t.path("id").asText();
        String tTitle = t.path("title").asText();
        String tDesc = t.path("description").asText("");
        String tDue = t.path("dueDateText").asText(t.path("dueDate").asText(""));
        boolean completed = t.path("isCompleted").asBoolean();

        if ((tTitle != null && tTitle.toLowerCase().contains(query)) ||
            (tDesc != null && tDesc.toLowerCase().contains(query))) {
            SearchResultDto tItem = new SearchResultDto();
            tItem.setId(tId);
            tItem.setModuleId("career");
            tItem.setModuleLabel("Career");
            tItem.setCategory("Career Task");
            tItem.setTitle(tTitle);
            tItem.setSubtitle((milestoneName != null ? milestoneName : "Career Roadmap") + (completed ? " · (Completed)" : ""));
            tItem.setSnippet(tDesc);
            tItem.setStatus(completed ? "COMPLETED" : "PENDING");
            tItem.setBadge(t.path("priority").asText("NORMAL"));
            tItem.setIcon(completed ? "task_alt" : "check_box_outline_blank");
            tItem.setDate(tDue);
            tItem.addMeta("taskId", tId);
            list.add(tItem);
        }

        // Subtasks recursion
        JsonNode subtasks = t.path("subtasks");
        if (subtasks.isArray()) {
            for (JsonNode st : subtasks) {
                searchCareerTaskNode(st, tTitle, query, list);
            }
        }
    }

    // =========================================================================
    // 7. INTELLIGENCE MODULE SEARCH
    // =========================================================================
    public List<SearchResultDto> searchIntelligence(String query) {
        List<SearchResultDto> list = new ArrayList<>();
        String pattern = "%" + query.toLowerCase() + "%";
        MapSqlParameterSource params = new MapSqlParameterSource("pattern", pattern);

        // A. Articles
        try {
            String sqlArticles = """
                SELECT id, title, description, author, source_name, area_of_interest, published_at, canonical_url
                FROM articles
                WHERE is_deleted = false
                  AND (LOWER(title) LIKE :pattern 
                    OR LOWER(COALESCE(description, '')) LIKE :pattern 
                    OR LOWER(COALESCE(author, '')) LIKE :pattern 
                    OR LOWER(COALESCE(source_name, '')) LIKE :pattern 
                    OR LOWER(COALESCE(area_of_interest, '')) LIKE :pattern)
                ORDER BY published_at DESC NULLS LAST, first_seen_at DESC LIMIT 20
            """;
            list.addAll(jdbc.query(sqlArticles, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("intelligence");
                item.setModuleLabel("Intelligence");
                item.setCategory("Article");
                item.setTitle(rs.getString("title"));
                String src = rs.getString("source_name");
                String area = rs.getString("area_of_interest");
                item.setSubtitle(String.format("%s%s", src != null ? src + " · " : "", area != null ? area : "General Signal"));
                item.setSnippet(rs.getString("description"));
                item.setStatus("CURATED");
                item.setBadge(area);
                item.setIcon("article");
                item.setDate(formatTimestamp(rs.getTimestamp("published_at")));
                item.addMeta("url", rs.getString("canonical_url"));
                return item;
            }));
        } catch (Exception e) {
            log.error("Intelligence articles search error: {}", e.getMessage());
        }

        // B. RSS Feeds
        try {
            String sqlFeeds = """
                SELECT id, name, feed_url, area_of_interest, description
                FROM rss_feeds
                WHERE LOWER(name) LIKE :pattern 
                   OR LOWER(COALESCE(description, '')) LIKE :pattern 
                   OR LOWER(COALESCE(area_of_interest, '')) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlFeeds, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("intelligence");
                item.setModuleLabel("Intelligence");
                item.setCategory("RSS Signal Feed");
                item.setTitle(rs.getString("name"));
                item.setSubtitle(rs.getString("area_of_interest") + " · Active Node");
                item.setSnippet(rs.getString("description") != null ? rs.getString("description") : rs.getString("feed_url"));
                item.setStatus("ACTIVE");
                item.setBadge(rs.getString("area_of_interest"));
                item.setIcon("rss_feed");
                return item;
            }));
        } catch (Exception e) {
            log.error("Intelligence feeds search error: {}", e.getMessage());
        }

        // C. Areas of Interest
        try {
            String sqlAreas = """
                SELECT id, name, code, description
                FROM interest_categories
                WHERE LOWER(name) LIKE :pattern 
                   OR LOWER(code) LIKE :pattern
                LIMIT 10
            """;
            list.addAll(jdbc.query(sqlAreas, params, (rs, i) -> {
                SearchResultDto item = new SearchResultDto();
                item.setId(rs.getString("id"));
                item.setModuleId("intelligence");
                item.setModuleLabel("Intelligence");
                item.setCategory("Interest Area");
                item.setTitle(rs.getString("name"));
                item.setSubtitle("Code: " + rs.getString("code"));
                item.setSnippet(rs.getString("description"));
                item.setStatus("ACTIVE");
                item.setBadge(rs.getString("code"));
                item.setIcon("category");
                return item;
            }));
        } catch (Exception e) {
            log.error("Intelligence areas search error: {}", e.getMessage());
        }

        return list;
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().toString().substring(0, 10);
    }

    private String getModuleLabel(String moduleId) {
        if (moduleId == null) return "General";
        return switch (moduleId.toLowerCase()) {
            case "projects" -> "Projects";
            case "events" -> "Events";
            case "finance" -> "Finance";
            case "vehicles" -> "Vehicles";
            case "administration" -> "Administration";
            case "career" -> "Career";
            case "intelligence" -> "Intelligence";
            default -> Character.toUpperCase(moduleId.charAt(0)) + moduleId.substring(1);
        };
    }
}
