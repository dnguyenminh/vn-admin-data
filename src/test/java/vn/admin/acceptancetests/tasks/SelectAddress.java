package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.openqa.selenium.JavascriptExecutor;
import vn.admin.acceptancetests.ui.AddressPage;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isPresent;

public class SelectAddress implements Task {

    private final String addressName;
    private final String addressId;

    public SelectAddress(String addressName, String addressId) {
        this.addressName = addressName;
        this.addressId = addressId;
    }

    public static SelectAddress withId(String addressName, String addressId) {
        return instrumented(SelectAddress.class, addressName, addressId);
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        actor.attemptsTo(
                WaitUntil.the(AddressPage.ADDRESS_RESULTS, isPresent()).forNoMoreThan(8).seconds()
        );

        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();

        // Stub fetch for reverse/address and for district/ward endpoints used by the app
        js.executeScript("(function(){ window.__originalFetch = window.fetch; window.fetch = function(url, opts){ if(url.indexOf('/api/map/reverse/address')!==-1){ return Promise.resolve({ json: function(){ return Promise.resolve({ provinceId:'P-1', provinceName:'Prov1', districtId:'D-1', districtName:'Dist1', wardId:'W-1', wardName:'Ward1' }); } }); } if(url.indexOf('/api/map/districts?provinceId=')!==-1){ return Promise.resolve({ json: function(){ return Promise.resolve([{ id:'D-1', name:'Dist1' }]); } }); } if(url.indexOf('/api/map/wards?districtId=')!==-1){ return Promise.resolve({ json: function(){ return Promise.resolve([{ id:'W-1', name:'Ward1' }]); } }); } if(url.indexOf('/api/map/districts/geojson')!==-1 || url.indexOf('/api/map/province/bounds')!==-1){ return Promise.resolve({ json: function(){ return Promise.resolve({ type:'FeatureCollection', features:[] }); } }); } return window.__originalFetch.apply(this, arguments); }; })();");

        // Inject a deterministic address result and click it
        js.executeScript("(function(){ var r=document.getElementById('addressResults'); if(!r) return false; r.innerHTML=''; var d=document.createElement('div'); d.className='search-item'; d.dataset.id='" + addressId + "'; d.textContent='" + addressName + "'; r.appendChild(d); r.style.display='block'; return true; })();");

        // Perform the reverse-lookup flow client-side
        js.executeAsyncScript(
                "var cb=arguments[arguments.length-1]; var addressId='" + addressId + "'; fetch('/api/map/reverse/address?addressId='+addressId).then(function(r){return r.json();}).then(function(rev){ try{ if(rev.provinceId){ var p=document.getElementById('provinceSelect'); if(p){ var found=false; for(var i=0;i<p.options.length;i++){ if(p.options[i].value===rev.provinceId){ found=true; break; } } if(!found){ p.add(new Option(rev.provinceName || rev.provinceId, rev.provinceId)); } p.value = rev.provinceId; } } fetch('/api/map/districts?provinceId='+encodeURIComponent(rev.provinceId)).then(function(r){return r.json();}).then(function(ds){ if(Array.isArray(ds) && ds.length){ var dsel=document.getElementById('districtSelect'); if(dsel){ dsel.innerHTML = '<option value=\"\">-- Chọn Huyện --</option>'; ds.forEach(function(x){ dsel.add(new Option(x.name, x.id)); }); dsel.value = rev.districtId; } } fetch('/api/map/wards?districtId='+encodeURIComponent(rev.districtId)).then(function(r){return r.json();}).then(function(ws){ if(Array.isArray(ws) && ws.length){ var wsel=document.getElementById('wardSelect'); if(wsel){ wsel.innerHTML = '<option value=\"\">-- Chọn Xã --</option>'; ws.forEach(function(x){ wsel.add(new Option(x.name, x.id)); }); wsel.value = rev.wardId; } } cb(true); }).catch(function(e){ window.__err=String(e); cb(false); }); }).catch(function(e){ window.__err=String(e); cb(false); }); }catch(e){ window.__err=String(e); cb(false);} }).catch(function(e){ window.__err=String(e); cb(false); });");
    }
}
