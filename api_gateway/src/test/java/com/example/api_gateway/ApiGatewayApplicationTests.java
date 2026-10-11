package com.example.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

/** Verifica que el Gateway carga las 9 rutas y que cada una apunta a un nombre de Eureka (lb://), no a un puerto. */
@SpringBootTest(properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTests {

	@Autowired
	private RouteLocator routeLocator;

	@Test
	void cadaRutaApuntaAlServicioRegistradoEnEureka() {
		Map<String, String> rutas = routeLocator.getRoutes()
				.collectMap(Route::getId, ruta -> ruta.getUri().toString())
				.block();

		assertThat(rutas).containsEntry("pacientes", "lb://pacientes")
				.containsEntry("citas", "lb://citas")
				.containsEntry("atenciones", "lb://atenciones")
				.containsEntry("laboratorio", "lb://laboratorio")
				.containsEntry("hospitalizacion", "lb://hospitalizacion")
				.containsEntry("farmacia", "lb://farmacia")
				.containsEntry("facturacion-pagos", "lb://facturacion-pagos")
				.containsEntry("notificaciones", "lb://notificaciones")
				.containsEntry("auditoria", "lb://auditoria");
		assertThat(rutas.values().stream().collect(Collectors.toSet())).allMatch(uri -> uri.startsWith("lb://"));
	}

}
