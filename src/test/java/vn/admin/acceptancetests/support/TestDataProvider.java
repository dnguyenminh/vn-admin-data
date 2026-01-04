package vn.admin.acceptancetests.support;

import io.cucumber.java.Before;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestDataProvider {

    @Before(order = 0)
    public void loadTestData() {
        String dbUrl = System.getenv().getOrDefault("DB_URL", System.getProperty("DB_URL", "jdbc:postgresql://localhost:5432/gis"));
        String dbUser = System.getenv().getOrDefault("DB_USER", System.getProperty("DB_USER", "postgres"));
        String dbPass = System.getenv().getOrDefault("DB_PASSWORD", System.getProperty("DB_PASSWORD", "postgres"));

        try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            loadDistinctApplIds(c);
            loadRecentCheckin(c);
            prefetchFcCheckins(c);
            loadNonExactMarker(c);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to DB for acceptance test data: " + e.getMessage(), e);
        }
    }

    private void loadDistinctApplIds(Connection c) throws SQLException {
        List<String> appls = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT appl_id FROM customer_address WHERE appl_id IS NOT NULL LIMIT 5")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) appls.add(rs.getString(1));
            }
        }
        if (!appls.isEmpty()) TestContext.getInstance().setDistinctApplIds(appls);

        // For each appl_id, try to pick a representative address string and fc_id from recent checkins so tests can select them
        for (String appl : appls) {
            try (PreparedStatement ps2 = c.prepareStatement("SELECT checkin_address, fc_id FROM checkin_address WHERE appl_id = ? AND checkin_address IS NOT NULL LIMIT 1")) {
                ps2.setString(1, appl);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        String addr = rs2.getString(1);
                        String fc = rs2.getString(2);
                        if (addr != null && !addr.isEmpty()) TestContext.getInstance().putSampleAddressForAppl(appl, addr);
                        if (fc != null && !fc.isEmpty()) TestContext.getInstance().putSampleFcForAppl(appl, fc);
                    }
                }
            }
        }
    }

    private void loadRecentCheckin(Connection c) throws SQLException {
        String sql = "SELECT id, appl_id, fc_id, customer_address_id, field_lat, field_long, checkin_date, checkin_address, distance FROM checkin_address WHERE field_lat IS NOT NULL AND field_long IS NOT NULL ORDER BY checkin_date DESC NULLS LAST LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String applId = rs.getString("appl_id");
                    String fcId = rs.getString("fc_id");
                    String custAddrId = rs.getString("customer_address_id");
                    double lat = rs.getDouble("field_lat");
                    double lng = rs.getDouble("field_long");
                    String date = rs.getString("checkin_date");
                    String checkinAddr = rs.getString("checkin_address");
                    double distanceVal = rs.getDouble("distance");

                    String geoJson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"id\":\"" + id + "\",\"appl_id\":\"" + (applId==null?"":applId) + "\",\"fc_id\":\"" + (fcId==null?"":fcId) + "\",\"customer_address_id\":\"" + (custAddrId==null?"":custAddrId) + "\",\"checkin_date\":\"" + (date==null?"":date) + "\"},\"geometry\":{\"type\":\"Point\",\"coordinates\": [" + lng + "," + lat + "]}}]}";
                    String popupHtml = "<div><strong>fc_id:</strong> " + escape(checkin(rs, "fc_id")) + "<br/><strong>appl_id:</strong> " + escape(checkin(rs, "appl_id")) + "<br/><strong>customer_address_id:</strong> " + escape(checkin(rs, "customer_address_id")) + "<br/><strong>date:</strong> " + escape(checkin(rs, "checkin_date")) + "<br/><strong>distance (m):</strong> " + (Double.isNaN(distanceVal)?"240":String.valueOf((int)distanceVal)) + "<br/><div style=\"margin-top:6px;font-size:12px;\"><strong>Administrative:</strong><br/>" + escape(checkin(rs, "checkin_address")) + "</div></div>";

                    TestContext.getInstance().setRecentCheckinGeoJson(geoJson);
                    TestContext.getInstance().setRecentCheckinPopupHtml(popupHtml);
                } else {
                    // No DB rows found; provide a deterministic synthetic recent checkin so UI tests remain deterministic
                    String geoJson = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{\"id\":\"SYN-1\",\"appl_id\":\"T-LOCAL\",\"fc_id\":\"FC_TEST\",\"customer_address_id\":\"ADDR-1\",\"checkin_date\":\"1970-01-01\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[105.001,10.002]}}]}";
                    String popupHtml = "<div><strong>fc_id:</strong> FC_TEST<br/><strong>appl_id:</strong> T-LOCAL<br/><strong>customer_address_id:</strong> ADDR-1<br/><strong>date:</strong> 1970-01-01<br/><strong>distance (m):</strong> 240<br/><div style=\"margin-top:6px;font-size:12px;\"><strong>Administrative:</strong><br/>Province: Unknown<br/>District: Unknown<br/>Ward: Unknown<br/></div></div>";
                    TestContext.getInstance().setRecentCheckinGeoJson(geoJson);
                    TestContext.getInstance().setRecentCheckinPopupHtml(popupHtml);
                }
            }
        }
    }

    private void prefetchFcCheckins(Connection c) throws SQLException {
        // Prefetch up to 20 fc_ids and their recent checkins up to 50 rows
        try (PreparedStatement psFc = c.prepareStatement("SELECT DISTINCT fc_id FROM checkin_address WHERE fc_id IS NOT NULL LIMIT 20")) {
            try (ResultSet rs = psFc.executeQuery()) {
                while (rs.next()) {
                    String fc = rs.getString(1);
                    if (fc == null) continue;
                    try (PreparedStatement ps = c.prepareStatement("SELECT id, appl_id, customer_address_id, field_lat, field_long, checkin_date FROM checkin_address WHERE fc_id = ? AND field_lat IS NOT NULL AND field_long IS NOT NULL ORDER BY checkin_date DESC NULLS LAST LIMIT 50")) {
                        ps.setString(1, fc);
                        try (ResultSet cr = ps.executeQuery()) {
                            StringBuilder feat = new StringBuilder();
                            int count = 0;
                            while (cr.next()) {
                                String id = cr.getString("id");
                                String appl = cr.getString("appl_id");
                                String cust = cr.getString("customer_address_id");
                                double lat = cr.getDouble("field_lat");
                                double lng = cr.getDouble("field_long");
                                String date = cr.getString("checkin_date");
                                if (count > 0) feat.append(',');
                                feat.append("{\"type\":\"Feature\",\"properties\":{\"id\":\"").append(id==null?"":id).append("\",\"fc_id\":\"").append(fc).append("\",\"customer_address_id\":\"").append(cust==null?"":cust).append("\",\"appl_id\":\"").append(appl==null?"":appl).append("\",\"checkin_date\":\"").append(date==null?"":date).append("\"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[").append(lng).append(',').append(lat).append("]}}");
                                count++;
                            }
                            if (count > 0) {
                                String geoJson = "{\"type\":\"FeatureCollection\",\"features\": [" + feat.toString() + "]}";
                                TestContext.getInstance().putFcGeoJson(fc, geoJson);
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadNonExactMarker(Connection c) throws SQLException {
        String sql = "SELECT customer_address_id, field_lat, field_long FROM checkin_address WHERE field_lat IS NOT NULL AND field_long IS NOT NULL AND customer_address_id IS NOT NULL ORDER BY checkin_date DESC NULLS LAST LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("customer_address_id");
                    double lat = rs.getDouble("field_lat");
                    double lng = rs.getDouble("field_long");
                    TestContext.getInstance().setNonExactMarker(id, lat, lng);
                }
            }
        }
    }

    // small utility because ResultSet.getString with missing column throws, so we guard
    private String checkin(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return ""; }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
