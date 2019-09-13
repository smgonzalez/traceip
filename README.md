# TraceIP

Recopilador de informacion de una IP
- Pais de origen
- Informacion general del pais origen
- Cotizacion en U$D de la moneda del pais
- Distancia del pais a Bs As


### Build con Docker

`$ docker build -t traceip/traceip .`

### Crear y ejecutar container Docker

`$ docker run -t -i -p 8080:8080 traceip/traceip`

### URLs

Consultar IP: http://localhost:8080<br>
Estadisticas de distancias: http://localhost:8080/ip-statistics
