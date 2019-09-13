# TraceIP

Recopilador de informacion de una IP
- Informacion general
- Cotizacion en U$D
- Distancias del pais origen, a Bs As


### Build con Docker

`$ docker build -t traceip/traceip .`

### Crear y ejecutar container Docker

`$ docker run -t -i -p 8080:8080 traceip/traceip`

### Accesos

Consultar IP: http://localhost:8080
Estadisticas de uso: http://localhost:8080/ip-statistics
