package com.monday.app.finance.repository;

import com.monday.app.finance.entity.Investment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InvestmentRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public InvestmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Investment save(Investment inv) {
        UUID id = inv.getId() != null ? inv.getId() : UUID.randomUUID();
        String sql = """
                INSERT INTO investments (id, name, investment_type, asset_class, instrument,
                    invested_amount, current_value, sip_amount, sip_day,
                    start_date, end_date, units, nav, folio_number, broker, notes, status, account_id)
                VALUES (:id, :name, :investmentType, :assetClass, :instrument,
                    :investedAmount, :currentValue, :sipAmount, :sipDay,
                    :startDate, :endDate, :units, :nav, :folioNumber, :broker, :notes, :status, :accountId)
                RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(id, inv), InvestmentRowMapper.INSTANCE);
    }

    public Investment update(Investment inv) {
        String sql = """
                UPDATE investments SET
                    name = :name, investment_type = :investmentType, asset_class = :assetClass,
                    instrument = :instrument, invested_amount = :investedAmount, current_value = :currentValue,
                    sip_amount = :sipAmount, sip_day = :sipDay, start_date = :startDate, end_date = :endDate,
                    units = :units, nav = :nav, folio_number = :folioNumber, broker = :broker,
                    notes = :notes, status = :status, account_id = :accountId
                WHERE id = :id RETURNING *
                """;
        return jdbc.queryForObject(sql, toParams(inv.getId(), inv), InvestmentRowMapper.INSTANCE);
    }

    public Optional<Investment> findById(UUID id) {
        return jdbc.query("SELECT * FROM investments WHERE id = :id",
                new MapSqlParameterSource("id", id), InvestmentRowMapper.INSTANCE)
                .stream().findFirst();
    }

    public List<Investment> findAll() {
        return jdbc.query("SELECT * FROM investments ORDER BY created_at DESC",
                InvestmentRowMapper.INSTANCE);
    }

    public List<Investment> findByType(String type) {
        return jdbc.query("SELECT * FROM investments WHERE investment_type = :type ORDER BY created_at DESC",
                new MapSqlParameterSource("type", type.toUpperCase()), InvestmentRowMapper.INSTANCE);
    }

    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM investments WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(UUID id, Investment inv) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", inv.getName())
                .addValue("investmentType", inv.getInvestmentType() != null ? inv.getInvestmentType().toUpperCase() : "ONE_TIME")
                .addValue("assetClass", inv.getAssetClass() != null ? inv.getAssetClass().toUpperCase() : "EQUITY")
                .addValue("instrument", inv.getInstrument())
                .addValue("investedAmount", inv.getInvestedAmount() != null ? inv.getInvestedAmount() : java.math.BigDecimal.ZERO)
                .addValue("currentValue", inv.getCurrentValue() != null ? inv.getCurrentValue() : java.math.BigDecimal.ZERO)
                .addValue("sipAmount", inv.getSipAmount())
                .addValue("sipDay", inv.getSipDay())
                .addValue("startDate", inv.getStartDate() != null ? Date.valueOf(inv.getStartDate()) : null)
                .addValue("endDate", inv.getEndDate() != null ? Date.valueOf(inv.getEndDate()) : null)
                .addValue("units", inv.getUnits())
                .addValue("nav", inv.getNav())
                .addValue("folioNumber", inv.getFolioNumber())
                .addValue("broker", inv.getBroker())
                .addValue("notes", inv.getNotes())
                .addValue("status", inv.getStatus() != null ? inv.getStatus().toUpperCase() : "ACTIVE")
                .addValue("accountId", inv.getAccountId());
    }

    static final class InvestmentRowMapper implements RowMapper<Investment> {
        static final InvestmentRowMapper INSTANCE = new InvestmentRowMapper();

        @Override
        public Investment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Investment inv = new Investment();
            inv.setId(UUID.fromString(rs.getString("id")));
            inv.setName(rs.getString("name"));
            inv.setInvestmentType(rs.getString("investment_type"));
            inv.setAssetClass(rs.getString("asset_class"));
            inv.setInstrument(rs.getString("instrument"));
            inv.setInvestedAmount(rs.getBigDecimal("invested_amount"));
            inv.setCurrentValue(rs.getBigDecimal("current_value"));
            inv.setSipAmount(rs.getBigDecimal("sip_amount"));
            int sipDay = rs.getInt("sip_day");
            if (!rs.wasNull()) inv.setSipDay(sipDay);
            Date sd = rs.getDate("start_date");
            if (sd != null) inv.setStartDate(sd.toLocalDate());
            Date ed = rs.getDate("end_date");
            if (ed != null) inv.setEndDate(ed.toLocalDate());
            inv.setUnits(rs.getBigDecimal("units"));
            inv.setNav(rs.getBigDecimal("nav"));
            inv.setFolioNumber(rs.getString("folio_number"));
            inv.setBroker(rs.getString("broker"));
            inv.setNotes(rs.getString("notes"));
            inv.setStatus(rs.getString("status"));
            String accId = rs.getString("account_id");
            if (accId != null) inv.setAccountId(UUID.fromString(accId));
            if (rs.getTimestamp("created_at") != null)
                inv.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            if (rs.getTimestamp("updated_at") != null)
                inv.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return inv;
        }
    }
}
