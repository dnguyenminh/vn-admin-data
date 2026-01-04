package vn.admin.acceptancetests.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.JavascriptExecutor;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InjectPolygonAndClickablePoint implements Task {
    public static InjectPolygonAndClickablePoint now() { return instrumented(InjectPolygonAndClickablePoint.class); }

    @Override
    public <T extends Actor> void performAs(T actor) {
        JavascriptExecutor js = (JavascriptExecutor) BrowseTheWeb.as(actor).getDriver();
        ((JavascriptExecutor) BrowseTheWeb.as(actor).getDriver()).executeScript("(function(){ try{ var m = (window.app && window.app.map && window.app.map.map) ? window.app.map.map : window.__testMap; if(m.createPane){ try{ m.createPane('markerPane'); var p = m.getPane('markerPane'); if(p) p.style.zIndex = 650; }catch(pe){} } var poly1 = L.polygon([[10.6,105.0],[10.6,105.2],[10.8,105.2],[10.8,105.0]]); var poly2 = L.polygon([[10.55,104.95],[10.55,105.25],[10.85,105.25],[10.85,104.95]]); if(window.app && window.app.map && window.app.map.provinceLayer){ window.app.map.provinceLayer.addLayer(poly1); } else { poly1.addTo(m); } if(window.app && window.app.map && window.app.map.districtLayer){ window.app.map.districtLayer.addLayer(poly2); } else { poly2.addTo(m); } var marker = L.circleMarker([10.7,105.1],{pane:'markerPane',radius:6,color:'#ff0000'}).bindPopup('Test Point'); if(window.app && window.app.map && window.app.map.addressLayer){ window.app.map.addressLayer.addLayer(marker); } else { marker.addTo(m); } return true; }catch(e){ return false; } })();");
    }
}
