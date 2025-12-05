package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import es.uca.orderflow.business.services.ClienteSesionService;
import es.uca.orderflow.business.services.EstadisticasConfigService;
import es.uca.orderflow.business.services.EstadisticasQueryService;
import es.uca.orderflow.business.services.dto.*;
import es.uca.orderflow.presentation.components.HtmlCanvas;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import es.uca.orderflow.business.services.DuennoSesionService;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@PageTitle("Estadísticas")
@Route("/backoffice/estadisticas")
@AnonymousAllowed
@RequiredArgsConstructor
@CssImport("./styles/estadisticas.css")
public class EstadisticasAdminView extends VerticalLayout implements BeforeEnterObserver {

    private final EstadisticasConfigService configService;
    private final EstadisticasQueryService statsService;
    private final ClienteSesionService sesionService;
    private final  DuennoSesionService duennoSesionService;

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
    private final Div container = new Div();

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (duennoSesionService.getActual() == null) {
            // si no hay dueño logueado -> mandar al login de dueño
            event.forwardTo(DuennoLoginView.class);
        }
    }



    @PostConstruct
    void init() {
        setId("stats-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        add(buildTopBar(), container);
        UI.getCurrent().getPage().executeJs("""
  (()=>{
    const setH = () => {
      const bar = document.querySelector('.stats-topbar');
      const h = bar ? bar.offsetHeight : 64;
      document.documentElement.style.setProperty('--topbar-h', h + 'px');
    };
    setH();
    window.addEventListener('resize', setH, { passive: true });
  })();
""");

        render();
    }

    private Component buildTopBar() {
        Div bar = new Div();
        bar.addClassName("stats-topbar");

        Icon left = VaadinIcon.ANGLE_LEFT.create(); // o ARROW_LEFT
        Button back = new Button("Volver", left);   // <-- texto + icono
        back.addClassName("back-btn");
        back.addClickListener(e -> UI.getCurrent().navigate("/backoffice/duennopanel")); // ajusta ruta

        H2 title = new H2("Estadísticas");
        title.addClassName("stats-title");

        bar.add(back, title);
        return bar;
    }



    private void render() {
        container.removeAll();
        if (!configService.isUnlocked()) {
            renderPaywall();
        } else {
            renderDash();
        }
    }

    private void renderPaywall() {
        // contenedor centrado
        Div center = new Div();
        center.addClassName("paywall-center");

        Div wrap = new Div();
        wrap.addClassName("paywall");

        Div hero = new Div();
        hero.addClassName("paywall-hero");

        // Header
        Div top = new Div();
        top.addClassName("pay-top");
        Span badge = new Span("🔒 Estadísticas Pro");
        badge.addClassName("pay-badge");
        H3 title = new H3("Desbloquea el módulo de estadísticas");
        title.addClassName("pay-title");
        top.add(badge, title);

        Paragraph sub = new Paragraph(
                "Clientes top, meses con más pedidos y productos más vendidos en gráficas interactivas."
        );
        sub.addClassName("pay-sub");

        // Grid
        Div grid = new Div();
        grid.addClassName("pay-grid");

        // Columna izquierda (beneficios + CTA)
        Div left = new Div();
        Div feats = new Div(); feats.addClassName("features");
        left.add(
                feature("Comparativas por mes"),
                feature("Top clientes y productos"),
                feature("Descarga PNG de las gráficas"),
                feature("Modo oscuro y responsive")
        );

        Span price = new Span("Precio: " + euro.format(configService.getPriceCents() / 100.0));
        price.addClassName("price-pill");

        Button buy = new Button("Desbloquear ahora");
        buy.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
        buy.getStyle().set("fontWeight","900");
        buy.addClickListener(e -> {
            var usuario = sesionService.getActual();
            String email = usuario != null ? usuario.getCorreo() : "anon@orderflow.local";
            var res = configService.unlockAll(email, "tok_demo");
            if (res.success()) UI.getCurrent().getPage().executeJs("setTimeout(()=>location.reload(),250)");
            else com.vaadin.flow.component.notification.Notification.show("Pago rechazado: " + res.message());
        });
        Div priceCta = new Div(price, buy); priceCta.addClassName("price-cta");
        left.add(feats, priceCta);

        // Columna derecha (ilustración con CANVAS de preview)
        Div right = new Div(); right.addClassName("pay-illu");
        HtmlCanvas preview = new HtmlCanvas();
        preview.setId("payPreview");
        preview.setWidth("100%");
        preview.setHeight("180px");
        right.add(preview);

        grid.add(left, right);

        // Footer
        Div bottom = new Div(); bottom.addClassName("pay-bottom");
        Span guarantee = new Span("Sin permanencia. Si no te convence, te devolvemos el dinero en 7 días.");
        guarantee.addClassName("guarantee");
        Anchor faq = new Anchor("#", "¿Tienes un código de descuento?"); faq.addClassName("subtle-link");
        bottom.add(guarantee, faq);

        hero.add(top, sub, grid, bottom);
        wrap.add(hero);
        center.add(wrap);
        container.add(center);

        // --- Mini gráfica de preview ---
        UI.getCurrent().getPage().executeJs("""
      (async ()=>{
        if(!window.Chart){
          await new Promise((ok,ko)=>{const s=document.createElement('script');s.src='https://cdn.jsdelivr.net/npm/chart.js';s.onload=ok;s.onerror=ko;document.head.appendChild(s);});
        }
        const css = getComputedStyle(document.documentElement);
        const primary = css.getPropertyValue('--stats-primary').trim();
        const success = css.getPropertyValue('--stats-success').trim();
        const c = document.getElementById('payPreview'); if(!c) return;
        const ctx = c.getContext('2d');

        const g1 = ctx.createLinearGradient(0,0,0,180);
        g1.addColorStop(0, primary+'55'); g1.addColorStop(1, primary+'10');
        const g2 = ctx.createLinearGradient(0,0,0,180);
        g2.addColorStop(0, success+'55'); g2.addColorStop(1, success+'08');

        const labels = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
        const pedidos = [3,5,4,7,8,6,9,11,10,13,12,15];
        const ingresos = [120,180,160,220,260,210,290,340,320,380,360,420];

        new Chart(ctx,{
          type:'bar',
          data:{ labels,
            datasets:[
              { type:'bar', label:'Pedidos', data: pedidos, backgroundColor:g1, borderColor:primary, borderRadius:6, maxBarThickness:22 },
              { type:'line', label:'Ingresos', data: ingresos, yAxisID:'y2', borderColor:success, backgroundColor:g2, fill:true, tension:.35, pointRadius:0, borderWidth:3 }
            ]},
          options:{
            responsive:true, maintainAspectRatio:false, plugins:{ legend:{display:false} },
            scales:{ x:{grid:{display:false}},
                     y:{beginAtZero:true, ticks:{display:false}, grid:{display:false}},
                     y2:{position:'right', beginAtZero:true, ticks:{display:false}, grid:{display:false}} }
          }
        });
      })();
    """);
    }


    // helper de feature con check
    private Component feature(String txt){
        Div f = new Div(); f.addClassName("feature");
        Span chk = new Span("✓"); chk.addClassName("chk");
        Paragraph p = new Paragraph(txt); p.getStyle().set("margin","0");
        f.add(chk, p);
        return f;
    }


    private void renderDash() {
        Div shell = new Div();
        shell.addClassName("stats-shell");

        var topClientes = statsService.topClientes(5);
        var ultimosMeses = statsService.pedidosPorMesUltimos12();
        var topProductos = statsService.topProductos(5);



        if (topClientes.isEmpty()) shell.add(empty("Aún no hay clientes con pedidos suficientes."));
        if (ultimosMeses.isEmpty()) shell.add(empty("No hay datos de meses recientes."));
        if (topProductos.isEmpty()) shell.add(empty("Todavía no hay productos vendidos."));

        shell.add(kpiRow(topClientes, ultimosMeses, topProductos));
        Div grid = new Div();
        grid.addClassName("chart-grid");


// ✅ estos en columnas
        grid.add(
                chartCard("Clientes con más pedidos", "chartClientes"),
                chartCard("Productos más vendidos", "chartProductos")
        );

        //  éste a ancho completo
        grid.add(chartCard("Pedidos por mes (últimos 12)", "chartMeses", true));

        shell.add(grid);

        container.add(shell);


        // JS mejorado
        UI.getCurrent().getPage().executeJs("""
      
       (async ()=>{
                             if(!window.Chart){
                               await new Promise((ok,ko)=>{const s=document.createElement('script');s.src='https://cdn.jsdelivr.net/npm/chart.js';s.onload=ok;s.onerror=ko;document.head.appendChild(s);});
                             }
                             // Defaults para resaltar
                             Chart.defaults.font.weight = 700;
                             Chart.defaults.elements.line.borderWidth = 3;
                             Chart.defaults.elements.point.radius = 0;
                             Chart.defaults.datasets.bar.borderSkipped = false;
                           })();
      (async (clientes, meses, prods)=>{
      
        const css = getComputedStyle(document.documentElement);
        const ink = css.getPropertyValue('--stats-ink').trim();
        const muted = css.getPropertyValue('--stats-muted').trim();
        const primary = css.getPropertyValue('--stats-primary').trim();
        const success = css.getPropertyValue('--stats-success').trim();
        const warn = css.getPropertyValue('--stats-warn').trim();

        if(!window.Chart){
          await new Promise((ok,ko)=>{
            const s=document.createElement('script');
            s.src='https://cdn.jsdelivr.net/npm/chart.js';
            s.onload=ok; s.onerror=ko; document.head.appendChild(s);
          });
        }

        const common = {
          responsive:true, maintainAspectRatio:false,
          plugins:{ legend:{display:false}, tooltip:{mode:'index', intersect:false} },
          scales:{
            x:{ grid:{display:false}, ticks:{color:muted} },
            y:{ grid:{color:'rgba(0,0,0,.06)'}, ticks:{color:muted}, beginAtZero:true }
          }
        };

        // helper gradient
        const grad = (ctx, from, to)=>{ const g=ctx.createLinearGradient(0,0,0,340); g.addColorStop(0,from); g.addColorStop(1,to); return g };

        // Clientes (bar redondeado)
        {
          const c = document.getElementById('chartClientes');
          const ctx = c.getContext('2d');
          new Chart(ctx,{
            type:'bar',
            data:{
              labels: clientes.map(c=>c.nombre),
              datasets:[{
                data: clientes.map(c=>c.pedidos),
                backgroundColor: grad(ctx, primary+'33', primary+'11'),
                borderColor: primary, borderWidth:2, borderRadius:8, maxBarThickness:48
              }]
            },
            options: common
          });
        }

        // Meses (línea con área)
        {
          const c = document.getElementById('chartMeses');
          const ctx = c.getContext('2d');
          const lbl = meses.map(m=>m.periodoYYYYMM).reverse();
          const dat = meses.map(m=>m.pedidos).reverse();
          new Chart(ctx,{
            type:'line',
            data:{
              labels: lbl,
              datasets:[{
                data: dat, tension:.35, fill:true,
                borderColor: success, backgroundColor: grad(ctx, success+'33', success+'05'), pointRadius:2
              }]
            },
            options: common
          });
        }

        // Productos (bar amarillo)
        {
          const c = document.getElementById('chartProductos');
          const ctx = c.getContext('2d');
          new Chart(ctx,{
            type:'bar',
            data:{
              labels: prods.map(p=>p.nombre),
              datasets:[{
                data: prods.map(p=>p.unidades),
                backgroundColor: grad(ctx, warn+'33', warn+'11'),
                borderColor: warn, borderWidth:2, borderRadius:8, maxBarThickness:48
              }]
            },
            options: common
          });
        }

        // Acciones: descargar PNG
        for (const dn of document.querySelectorAll('[data-dn]')) {
          dn.addEventListener('click', ()=>{
            const id = dn.getAttribute('data-dn');
            const cnv = document.getElementById(id);
            const a = document.createElement('a');
            a.download = id+'.png'; a.href = cnv.toDataURL('image/png'); a.click();
          });
        }
      })($0,$1,$2);
    """,
                toJsonClientes(topClientes),
                toJsonMeses(ultimosMeses),
                toJsonProductos(topProductos)
        );
    }


    private JsonArray toJsonClientes(List<TopClienteDTO> list) {
        JsonArray arr = Json.createArray();
        for (int i = 0; i < list.size(); i++) {
            TopClienteDTO x = list.get(i);
            JsonObject o = Json.createObject();
            o.put("nombre", x.nombre());
            o.put("pedidos", x.pedidos());
            arr.set(i, o);
        }
        return arr;
    }

    private JsonArray toJsonMeses(List<PedidosMesDTO> list) {
        JsonArray arr = Json.createArray();
        for (int i = 0; i < list.size(); i++) {
            PedidosMesDTO x = list.get(i);
            JsonObject o = Json.createObject();
            o.put("periodoYYYYMM", x.periodoYYYYMM());
            o.put("pedidos", x.pedidos());
            arr.set(i, o);
        }
        return arr;
    }
    private Component empty(String msg){
        Div e = new Div(); e.addClassName("empty"); e.add(new Paragraph(msg));
        return e;
    }


    private JsonArray toJsonProductos(List<TopProductoDTO> list) {
        JsonArray arr = Json.createArray();
        for (int i = 0; i < list.size(); i++) {
            TopProductoDTO x = list.get(i);
            JsonObject o = Json.createObject();
            o.put("nombre", x.nombre());
            o.put("unidades", x.unidades());
            o.put("importe", (false) ? 0.0 : x.importe());
            arr.set(i, o);
        }
        return arr;
    }

    // --- Layout helpers ---

    private Component kpiRow(List<TopClienteDTO> topClientes, List<PedidosMesDTO> ultimosMeses, List<TopProductoDTO> topProductos) {
        Div row = new Div(); row.addClassName("kpi-row");

        long totalPedidosUlt12 = ultimosMeses.stream().mapToLong(PedidosMesDTO::pedidos).sum();
        String topCliente = topClientes.isEmpty() ? "-" : topClientes.get(0).nombre();
        String topProducto = topProductos.isEmpty() ? "-" : topProductos.get(0).nombre();

        // delta último mes vs anterior
        String deltaTxt = "";
        boolean deltaUp = true;
        if (ultimosMeses.size() >= 2) {
            long last = ultimosMeses.get(ultimosMeses.size()-1).pedidos();
            long prev = ultimosMeses.get(ultimosMeses.size()-2).pedidos();
            if (prev > 0) {
                double d = ((last - prev) / (double)prev) * 100.0;
                deltaUp = d >= 0;
                deltaTxt = String.format("%+.0f%%", d);
            }
        }

        row.add(
                kpi("📦", "Pedidos (últ. 12 meses)", String.valueOf(totalPedidosUlt12), deltaTxt, deltaUp, "Suma de pedidos"),
                kpi("👤", "Cliente top", topCliente, "", true, "Mayor nº de pedidos"),
                kpi("🍽️", "Producto top", topProducto, "", true, "Más unidades vendidas")
        );
        return row;
    }

    // Archivo: EstadisticasAdminView.java

// ...

    private Component kpi(String emoji, String label, String value, String delta, boolean up, String sub) {
        Div c = new Div(); c.addClassName("kpi");
        Span ico = new Span(emoji); ico.addClassName("ico");

        Span l = new Span(label); l.addClassName("kpi-l");
        H3 v = new H3(value); v.getStyle().set("margin","0");
        SmallText s = new SmallText(sub); // helper simple
        Span d = new Span();
        if (!delta.isBlank()) {
            d.setText((up?"↑ ":"↓ ") + delta);


            d.addClassName("delta");
            if (!up) {
                d.addClassName("down");
            }

            Span arr = new Span(up ? "↑" : "↓"); arr.addClassName("arrow");
        }
        c.add(ico, l, v, new SmallText(sub));
        if (!delta.isBlank()) c.add(d);
        return c;
    }

    // ...
    private static class SmallText extends Span {
        SmallText(String txt){ super(txt); getStyle().set("display","block"); getStyle().set("marginTop","2px"); getStyle().set("color","var(--stats-muted)"); getStyle().set("fontSize",".78rem"); }
    }


    private Component kpi(String label, String value) {
        Div c = new Div();
        c.addClassName("kpi");
        Span l = new Span(label);
        l.addClassName("kpi-l");
        H3 v = new H3(value);
        v.getStyle().set("margin", "0");
        c.add(l, v);
        return c;
    }

    private Component chartCard(String title, String canvasId){
        return chartCard(title, canvasId, false);
    }

    private Component chartCard(String title, String canvasId, boolean span2){
        Div card = new Div();
        card.addClassName("chart-card");
        if (span2) card.addClassName("span-2");

        Div head = new Div(); head.addClassName("chart-head");
        H4 h = new H4(title); h.getStyle().set("margin","0");
        Div actions = new Div(); actions.addClassName("chart-actions");
        Button dn = new Button("Descargar PNG");
        dn.getElement().setAttribute("class","btn");
        dn.getElement().setAttribute("data-dn", canvasId);
        actions.add(dn);
        head.add(h, actions);

        HtmlCanvas cnv = new HtmlCanvas();
        cnv.setId(canvasId); cnv.setWidth("100%"); cnv.setHeight("320px");

        card.add(head, cnv);
        return card;
    }




}
