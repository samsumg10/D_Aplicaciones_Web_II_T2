-- Una base de datos por microservicio en el mismo servidor PostgreSQL (database-per-service).
-- Ningún servicio consulta la base de otro: comparten el servidor, no los datos.
-- Las tablas las crea cada servicio al iniciar con su propio schema.sql.
CREATE DATABASE hospital_pacientes;
CREATE DATABASE hospital_citas;
CREATE DATABASE hospital_atenciones;
CREATE DATABASE hospital_laboratorio;
CREATE DATABASE hospital_hospitalizacion;
CREATE DATABASE hospital_farmacia;
CREATE DATABASE hospital_facturacion;
CREATE DATABASE hospital_notificaciones;
CREATE DATABASE hospital_auditoria;
