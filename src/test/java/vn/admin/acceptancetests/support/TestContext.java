package vn.admin.acceptancetests.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestContext {
    private static final TestContext INSTANCE = new TestContext();

    private Map<String, String> fcGeoJson = new HashMap<>();
    private List<String> distinctApplIds = Collections.emptyList();
    private String recentCheckinGeoJson;
    private String recentCheckinPopupHtml;
    private String nonExactMarkerId;
    private Double nonExactLat;
    private Double nonExactLng;

    // sample address/fc mapping per appl_id to be used by tests
    private Map<String, String> sampleAddressByApplId = new HashMap<>();
    private Map<String, String> sampleFcByApplId = new HashMap<>();

    private TestContext() { }

    public static TestContext getInstance() {
        return INSTANCE;
    }

    public synchronized void putFcGeoJson(String fcId, String geoJson) { fcGeoJson.put(fcId, geoJson); }
    public synchronized String getFcGeoJson(String fcId) { return fcGeoJson.get(fcId); }

    public synchronized void setDistinctApplIds(List<String> ids) { this.distinctApplIds = ids; }
    public synchronized List<String> getDistinctApplIds() { return distinctApplIds; }

    public synchronized void setRecentCheckinGeoJson(String geoJson) { this.recentCheckinGeoJson = geoJson; }
    public synchronized String getRecentCheckinGeoJson() { return recentCheckinGeoJson; }

    public synchronized void setRecentCheckinPopupHtml(String html) { this.recentCheckinPopupHtml = html; }
    public synchronized String getRecentCheckinPopupHtml() { return recentCheckinPopupHtml; }

    public synchronized void setNonExactMarker(String id, Double lat, Double lng) { this.nonExactMarkerId = id; this.nonExactLat = lat; this.nonExactLng = lng; }
    public synchronized String getNonExactMarkerId() { return nonExactMarkerId; }
    public synchronized Double getNonExactLat() { return nonExactLat; }
    public synchronized Double getNonExactLng() { return nonExactLng; }

    public synchronized void putSampleAddressForAppl(String applId, String address) { if (applId!=null) sampleAddressByApplId.put(applId, address); }
    public synchronized String getSampleAddressForAppl(String applId) { return sampleAddressByApplId.get(applId); }

    public synchronized void putSampleFcForAppl(String applId, String fcId) { if (applId!=null) sampleFcByApplId.put(applId, fcId); }
    public synchronized String getSampleFcForAppl(String applId) { return sampleFcByApplId.get(applId); }

    public synchronized String getAnyFc() { return sampleFcByApplId.values().stream().findFirst().orElse(null); }
}
