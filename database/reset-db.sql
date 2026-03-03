-- Drop and recreate the database
DROP DATABASE IF EXISTS raincheque;
CREATE DATABASE raincheque;

-- Connect to the database
\c raincheque;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Run your schema and seed files
\i '01-schema.sql'
\i '02-seed-data.sql'