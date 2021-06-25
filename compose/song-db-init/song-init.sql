--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.12
-- Dumped by pg_dump version 9.6.12

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: access_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.access_type AS ENUM (
    'controlled',
    'open'
);


ALTER TYPE public.access_type OWNER TO postgres;

--
-- Name: analysis_state; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.analysis_state AS ENUM (
    'PUBLISHED',
    'UNPUBLISHED',
    'SUPPRESSED'
);


ALTER TYPE public.analysis_state OWNER TO postgres;

--
-- Name: analysis_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.analysis_type AS ENUM (
    'sequencingRead',
    'variantCall',
    'consensus_sequence',
    'MAF'
);


ALTER TYPE public.analysis_type OWNER TO postgres;

--
-- Name: file_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.file_type AS ENUM (
    'FASTA',
    'FAI',
    'FASTQ',
    'BAM',
    'BAI',
    'VCF',
    'TBI',
    'IDX',
    'XML',
    'TGZ'
);


ALTER TYPE public.file_type OWNER TO postgres;

--
-- Name: gender; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.gender AS ENUM (
    'male',
    'female',
    'unspecified'
);


ALTER TYPE public.gender OWNER TO postgres;

--
-- Name: id_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.id_type AS ENUM (
    'Study',
    'Donor',
    'Specimen',
    'Sample',
    'File',
    'Analysis',
    'SequencingRead',
    'consensus_sequence',
    'VariantCall'
);


ALTER TYPE public.id_type OWNER TO postgres;

--
-- Name: library_strategy; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.library_strategy AS ENUM (
    'WGS',
    'WXS',
    'RNA-Seq',
    'ChIP-Seq',
    'miRNA-Seq',
    'Bisulfite-Seq',
    'Validation',
    'Amplicon',
    'Other'
);


ALTER TYPE public.library_strategy OWNER TO postgres;

--
-- Name: sample_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.sample_type AS ENUM (
    'DNA',
    'FFPE DNA',
    'Amplified DNA',
    'RNA',
    'Total RNA',
    'FFPE RNA'
);


ALTER TYPE public.sample_type OWNER TO postgres;

--
-- Name: specimen_class; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.specimen_class AS ENUM (
    'Normal',
    'Tumour',
    'Adjacent normal'
);


ALTER TYPE public.specimen_class OWNER TO postgres;

--
-- Name: specimen_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.specimen_type AS ENUM (
    'Normal - solid tissue',
    'Normal - blood derived',
    'Normal - bone marrow',
    'Normal - tissue adjacent to primary',
    'Normal - buccal cell',
    'Normal - EBV immortalized',
    'Normal - lymph node',
    'Normal - other',
    'Primary tumour',
    'Primary tumour - solid tissue',
    'Primary tumour - blood derived (peripheral blood)',
    'Primary tumour - blood derived (bone marrow)',
    'Primary tumour - additional new primary',
    'Primary tumour - other',
    'Recurrent tumour - solid tissue',
    'Recurrent tumour - blood derived (peripheral blood)',
    'Recurrent tumour - blood derived (bone marrow)',
    'Recurrent tumour - other',
    'Metastatic tumour - NOS',
    'Metastatic tumour - lymph node',
    'Metastatic tumour - metastasis local to lymph node',
    'Metastatic tumour - metastasis to distant location',
    'Metastatic tumour - additional metastatic',
    'Xenograft - derived from primary tumour',
    'Xenograft - derived from tumour cell line',
    'Cell line - derived from tumour',
    'Primary tumour - lymph node',
    'Metastatic tumour - other',
    'Cell line - derived from xenograft tumour'
);


ALTER TYPE public.specimen_type OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: analysis; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis (
    id character varying(36) NOT NULL,
    study_id character varying(36),
    type public.analysis_type,
    state public.analysis_state,
    analysis_schema_id integer,
    analysis_data_id integer
);


ALTER TABLE public.analysis OWNER TO postgres;

--
-- Name: analysis_data; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis_data (
    id bigint NOT NULL,
    data jsonb NOT NULL
);


ALTER TABLE public.analysis_data OWNER TO postgres;

--
-- Name: analysis_data_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.analysis_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.analysis_data_id_seq OWNER TO postgres;

--
-- Name: analysis_data_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.analysis_data_id_seq OWNED BY public.analysis_data.id;


--
-- Name: analysis_schema; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis_schema (
    id bigint NOT NULL,
    version integer,
    name character varying(225) NOT NULL,
    schema jsonb NOT NULL
);


ALTER TABLE public.analysis_schema OWNER TO postgres;

--
-- Name: analysis_schema_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.analysis_schema_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.analysis_schema_id_seq OWNER TO postgres;

--
-- Name: analysis_schema_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.analysis_schema_id_seq OWNED BY public.analysis_schema.id;


--
-- Name: donor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.donor (
    id character varying(36) NOT NULL,
    study_id character varying(36),
    submitter_id text,
    gender public.gender
);


ALTER TABLE public.donor OWNER TO postgres;

--
-- Name: sample; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sample (
    id character varying(36) NOT NULL,
    specimen_id character varying(36),
    submitter_id text,
    type public.sample_type
);


ALTER TABLE public.sample OWNER TO postgres;

--
-- Name: specimen; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.specimen (
    id character varying(36) NOT NULL,
    donor_id character varying(36),
    submitter_id text,
    class public.specimen_class,
    type public.specimen_type
);


ALTER TABLE public.specimen OWNER TO postgres;

--
-- Name: study; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.study (
    id character varying(36) NOT NULL,
    name text,
    description text,
    organization text
);


ALTER TABLE public.study OWNER TO postgres;

--
-- Name: businesskeyview; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.businesskeyview AS
 SELECT s.id AS study_id,
    sp.id AS specimen_id,
    sp.submitter_id AS specimen_submitter_id,
    sa.id AS sample_id,
    sa.submitter_id AS sample_submitter_id
   FROM (((public.study s
     JOIN public.donor d ON (((s.id)::text = (d.study_id)::text)))
     JOIN public.specimen sp ON (((d.id)::text = (sp.donor_id)::text)))
     JOIN public.sample sa ON (((sp.id)::text = (sa.specimen_id)::text)));


ALTER TABLE public.businesskeyview OWNER TO postgres;

--
-- Name: file; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.file (
    id character varying(36) NOT NULL,
    analysis_id character varying(36),
    study_id character varying(36),
    name text,
    size bigint,
    md5 character(32),
    type public.file_type,
    access public.access_type
);


ALTER TABLE public.file OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: sampleset; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sampleset (
    analysis_id character varying(36),
    sample_id character varying(36)
);


ALTER TABLE public.sampleset OWNER TO postgres;

--
-- Name: idview; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.idview AS
 SELECT DISTINCT a.id AS analysis_id,
    ans.id AS analysis_schema_id,
    ans.name AS analysis_schema_name,
    a.state AS analysis_state,
    a.study_id,
    d.id AS donor_id,
    sp.id AS specimen_id,
    sa.id AS sample_id,
    f.id AS object_id
   FROM ((((((public.donor d
     JOIN public.specimen sp ON (((d.id)::text = (sp.donor_id)::text)))
     JOIN public.sample sa ON (((sp.id)::text = (sa.specimen_id)::text)))
     JOIN public.sampleset sas ON (((sa.id)::text = (sas.sample_id)::text)))
     JOIN public.file f ON (((sas.analysis_id)::text = (f.analysis_id)::text)))
     JOIN public.analysis a ON (((sas.analysis_id)::text = (a.id)::text)))
     JOIN public.analysis_schema ans ON ((a.analysis_schema_id = ans.id)));


ALTER TABLE public.idview OWNER TO postgres;

--
-- Name: info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.info (
    id character varying(36),
    id_type public.id_type,
    info json
);


ALTER TABLE public.info OWNER TO postgres;

--
-- Name: infoview; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.infoview AS
 SELECT a.id AS analysis_id,
    i_study.info AS study_info,
    i_donor.info AS donor_info,
    i_sp.info AS specimen_info,
    i_sa.info AS sample_info,
    i_a.info AS analysis_info,
    i_f.info AS file_info
   FROM ((((((((((((public.study s
     JOIN public.info i_study ON ((((i_study.id)::text = (s.id)::text) AND (i_study.id_type = 'Study'::public.id_type))))
     JOIN public.donor d ON (((s.id)::text = (d.study_id)::text)))
     JOIN public.info i_donor ON ((((i_donor.id)::text = (d.id)::text) AND (i_donor.id_type = 'Donor'::public.id_type))))
     JOIN public.specimen sp ON (((d.id)::text = (sp.donor_id)::text)))
     JOIN public.info i_sp ON ((((i_sp.id)::text = (sp.id)::text) AND (i_sp.id_type = 'Specimen'::public.id_type))))
     JOIN public.sample sa ON (((sp.id)::text = (sa.specimen_id)::text)))
     JOIN public.info i_sa ON ((((i_sa.id)::text = (sa.id)::text) AND (i_sa.id_type = 'Sample'::public.id_type))))
     JOIN public.sampleset ss ON (((sa.id)::text = (ss.sample_id)::text)))
     JOIN public.analysis a ON (((ss.analysis_id)::text = (a.id)::text)))
     JOIN public.info i_a ON ((((i_a.id)::text = (a.id)::text) AND (i_a.id_type = 'Analysis'::public.id_type))))
     JOIN public.file f ON (((a.id)::text = (f.analysis_id)::text)))
     JOIN public.info i_f ON ((((i_f.id)::text = (f.id)::text) AND (i_f.id_type = 'File'::public.id_type))));


ALTER TABLE public.infoview OWNER TO postgres;

--
-- Name: upload; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.upload (
    id character varying(40) NOT NULL,
    study_id character varying(36),
    analysis_id text,
    state character varying(50),
    errors text,
    payload text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.upload OWNER TO postgres;

--
-- Name: analysis_data id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_data ALTER COLUMN id SET DEFAULT nextval('public.analysis_data_id_seq'::regclass);


--
-- Name: analysis_schema id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_schema ALTER COLUMN id SET DEFAULT nextval('public.analysis_schema_id_seq'::regclass);


--
-- Data for Name: analysis; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis (id, study_id, type, state, analysis_schema_id, analysis_data_id) FROM stdin;
\.


--
-- Data for Name: analysis_data; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis_data (id, data) FROM stdin;
\.


--
-- Name: analysis_data_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.analysis_data_id_seq', 1, true);


--
-- Data for Name: analysis_schema; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.analysis_schema (id, version, name, schema) FROM stdin;
1	1	consensus_sequence	{"type":"object","required":["sample_collection","host","experiment","sequence_analysis","pathogen_diagnostic_testing"],"properties":{"sample_collection":{"type":"object","required":["sample_collected_by","sequence_submitted_by","sample_collection_date","sample_collection_date_null_reason","geo_loc_country","geo_loc_province","organism","isolate","fasta_header_name","purpose_of_sampling","purpose_of_sampling_details","anatomical_material","anatomical_part","body_product","environmental_material","environmental_site","collection_device","collection_method"],"properties":{"sample_collected_by":{"enum":["National Microbiology Laboratory (NML)","BCCDC Public Health Laboratory","Alberta Precision Labs (APL)","Alberta ProvLab North (APLN)","Alberta ProvLab South (APLS)","Public Health Ontario (PHO)","Laboratoire de santé publique du Québec (LSPQ)","Lake of the Woods District Hospital - Ontario","Saskatchewan - Roy Romanow Provincial Laboratory (RRPL)","Manitoba Cadham Provincial Laboratory","Nova Scotia Health Authority","New Brunswick - Vitalité Health Network","Newfoundland and Labrador - Eastern Health","Nunuvut","Prince Edward Island - Health PEI","Ontario Institute for Cancer Research (OICR)","McMaster University","William Osler Health System","Sunnybrook Health Sciences Centre","Eastern Ontario Regional Laboratory Association","St. John's Rehab at Sunnybrook Hospital","Mount Sinai Hospital","Hamilton Health Sciences","Unity Health Toronto","Queen’s University / Kingston Health Sciences Centre","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"sequence_submitted_by":{"enum":["National Microbiology Laboratory (NML)","BCCDC Public Health Laboratory","Alberta Precision Labs (APL)","Alberta ProvLab North (APLN)","Alberta ProvLab South (APLS)","Public Health Ontario (PHO)","Laboratoire de santé publique du Québec (LSPQ)","Saskatchewan - Roy Romanow Provincial Laboratory (RRPL)","Manitoba Cadham Provincial Laboratory","Nova Scotia Health Authority","New Brunswick - Vitalité Health Network","Newfoundland and Labrador - Eastern Health","Prince Edward Island - Health PEI","Ontario Institute for Cancer Research (OICR)","McMaster University","McGill University","The Centre for Applied Genomics (TCAG)","Sunnybrook Health Sciences Centre","Thunder Bay Regional Health Sciences Centre","Canadore College","Queen's University / Kingston Health Sciences Centre","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"sample_collection_date":{"anyOf":[{"type":"string","format":"date"},{"type":"null"}]},"sample_collection_date_precision":{"enum":["year","month","day"]},"sample_collection_date_null_reason":{"enum":["Not Applicable","Missing","Not Collected","Not Provided","Restricted Access",null]},"geo_loc_country":{"enum":["Canada","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"geo_loc_province":{"enum":["Alberta","British Columbia","Manitoba","New Brunswick","Newfoundland and Labrador","Northwest Territories","Nova Scotia","Nunavut","Ontario","Prince Edward Island","Quebec","Saskatchewan","Yukon","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"geo_loc_city":{"type":["string","null"]},"organism":{"enum":["Severe acute respiratory syndrome coronavirus 2","RaTG13","RmYN02","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"isolate":{"type":["string","null"]},"fasta_header_name":{"type":["string"]},"purpose_of_sampling":{"enum":["Cluster/Outbreak investigation","Diagnostic testing","Research","Surveillance","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"purpose_of_sampling_details":{"type":["string","null"]},"anatomical_material":{"enum":["Blood","Fluid","Saliva","Fluid (cerebrospinal (CSF))","Fluid (pericardial)","Fluid (pleural)","Fluid (vaginal)","Fluid (amniotic)","Tissue","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"anatomical_part":{"enum":["Anus","Buccal mucosa","Duodenum","Eye","Intestine","Rectum","Skin","Stomach","Upper respiratory tract","Anterior Nares","Esophagus","Ethmoid sinus","Nasal Cavity","Middle Nasal Turbinate","Inferior Nasal Turbinate","Nasopharynx (NP)","Oropharynx (OP)","Lower respiratory tract","Bronchus","Lung","Bronchiole","Alveolar sac","Pleural sac","Pleural cavity","Trachea","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"body_product":{"enum":["Feces","Urine","Sweat","Mucus","Sputum","Tear","Fluid (seminal)","Breast Milk","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"environmental_material":{"enum":["Air vent","Banknote","Bed rail","Building floor","Cloth","Control panel","Door","Door handle","Face mask","Face shield","Food","Food packaging","Glass","Handrail","Hospital gown","Light switch","Locker","N95 mask","Nurse call button","Paper","Particulate matter","Plastic","PPE gown","Sewage","Sink","Soil","Stainless steel","Tissue paper","Toilet bowl","Water","Wastewater","Window","Wood","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"environmental_site":{"enum":["Acute care facility","Animal house","Bathroom","Clinical assessment centre","Conference venue","Corridor","Daycare","Emergency room (ER)","Family practice clinic","Group home","Homeless shelter","Hospital","Intensive Care Unit (ICU)","Long Term Care Facility","Patient room","Prison","Production Facility","School","Sewage Plant","Subway train","Wet market","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"collection_device":{"enum":["Air filter","Blood Collection Tube","Bronchoscope","Collection Container","Collection Cup","Fibrobronchoscope Brush","Filter","Fine Needle","Microcapillary tube","Micropipette","Needle","Serum Collection Tube","Sputum Collection Tube","Suction Catheter","Swab","Urine Collection Tube","Virus Transport Medium","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"collection_method":{"enum":["Amniocentesis","Aspiration","Suprapubic Aspiration","Tracheal aspiration","Vacuum Aspiration","Biopsy","Needle Biopsy","Filtration","Air filtration","Lavage","Bronchoalveolar lavage (BAL)","Gastric Lavage","Lumbar Puncture","Necropsy","Phlebotomy","Rinsing","Saline gargle (mouth rinse and gargle)","Scraping","Swabbing","Finger Prick","Wash","Washout Tear Collection","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"nml_submitted_specimen_type":{"enum":["Swab","RNA","mRNA (cDNA)","Nucleic acid","Not Applicable"]}},"if":{"properties":{"sample_collection_date":{"const":null}}},"then":{"properties":{"sample_collection_date_null_reason":{"enum":["Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]}}},"else":{"properties":{"sample_collection_date_null_reason":{"const":null}}},"propertyNames":{"enum":["sample_collected_by","sequence_submitted_by","sample_collection_date","sample_collection_date_precision","sample_collection_date_null_reason","geo_loc_country","geo_loc_province","geo_loc_city","organism","isolate","fasta_header_name","purpose_of_sampling","purpose_of_sampling_details","anatomical_material","anatomical_part","body_product","environmental_material","environmental_site","collection_device","collection_method","nml_submitted_specimen_type"]}},"host":{"type":"object","required":["host_scientific_name","host_disease","host_age","host_age_null_reason","host_age_bin","host_age_unit","host_gender"],"properties":{"host_scientific_name":{"enum":["Homo sapiens","Bos taurus","Canis lupus familiaris","Chiroptera","Columbidae","Felis catus","Gallus gallus","Manis","Manis javanica","Neovison vison","Panthera leo","Panthera tigris","Rhinolophidae","Rhinolophus affinis","Sus scrofa domesticus","Viverridae","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"host_disease":{"enum":["COVID-19","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"host_age":{"anyOf":[{"type":"number","minimum":0,"maximum":130},{"type":"null"}]},"host_age_null_reason":{"enum":["Not Applicable","Missing","Not Collected","Not Provided","Restricted Access",null]},"host_age_bin":{"enum":["0 - 9","10 - 19","20 - 29","30 - 39","40 - 49","50 - 59","60 - 69","70 - 79","80 - 89","90 - 99","100+","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"host_gender":{"enum":["Female","Male","Non-binary gender","Transgender (Male to Female)","Transgender (Female to Male)","Undeclared","Unknown","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"host_age_unit":{"enum":["year","month","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]}},"if":{"properties":{"host_age":{"const":null}}},"then":{"properties":{"host_age_null_reason":{"enum":["Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]}}},"else":{"properties":{"host_age_null_reason":{"const":null}}},"propertyNames":{"enum":["host_scientific_name","host_disease","host_age","host_age_null_reason","host_age_bin","host_age_unit","host_gender"]}},"experiment":{"type":"object","required":["purpose_of_sequencing","purpose_of_sequencing_details","sequencing_instrument","sequencing_protocol"],"properties":{"purpose_of_sequencing":{"enum":["Baseline surveillance (random sampling)","Targeted surveillance (non-random sampling)","Priority surveillance project","Screening for Variants of Concern (VoC)","Longitudinal surveillance (repeat sampling of individuals)","Re-infection surveillance","Vaccine escape surveillance","Travel-associated surveillance","Domestic travel surveillance","International travel surveillance","Cluster/Outbreak investigation","Multi-jurisdictional outbreak investigation","Intra-jurisdictional outbreak investigation","Research","Viral passage experiment","Protocol testing experiment","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"purpose_of_sequencing_details":{"type":["string","null"]},"sequencing_instrument":{"enum":["Illumina","Illumina Genome Analyzer","Illumina Genome Analyzer II","Illumina Genome Analyzer IIx","Illumina HiScanSQ","Illumina HiSeq","Illumina HiSeq X","Illumina HiSeq X Five","Illumina HiSeq X Ten","Illumina HiSeq 1000","Illumina HiSeq 1500","Illumina HiSeq 2000","Illumina HiSeq 2500","Illumina HiSeq 3000","Illumina HiSeq 4000","Illumina iSeq","Illumina iSeq 100","Illumina NovaSeq","Illumina NovaSeq 6000","Illumina MiniSeq","Illumina MiSeq","Illumina NextSeq","Illumina NextSeq 500","Illumina NextSeq 550","Illumina NextSeq 2000","Pacific Biosciences","PacBio RS","PacBio RS II","PacBio Sequel","PacBio Sequel II","Ion Torrent","Ion Torrent PGM","Ion Torrent Proton","Ion Torrent S5 XL","Ion Torrent S5","Oxford Nanopore","Oxford Nanopore GridION","Oxford Nanopore MinION","Oxford Nanopore PromethION","BGI Genomics","BGI Genomics BGISEQ-500","MGI","MGI DNBSEQ-T7","MGI DNBSEQ-G400","MGI DNBSEQ-G400 FAST","MGI DNBSEQ-G50","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"sequencing_protocol":{"type":["string","null"]},"sequencing_date":{"anyOf":[{"type":"string","format":"date"},{"type":"null"}]},"library_id":{"type":["string","null"]}},"propertyNames":{"enum":["sequencing_instrument","sequencing_date","library_id","purpose_of_sequencing","purpose_of_sequencing_details","sequencing_protocol"]}},"sequence_analysis":{"type":"object","required":["consensus_sequence_software_name","consensus_sequence_software_version","raw_sequence_data_processing_method","dehosting_method"],"properties":{"consensus_sequence_software_name":{"type":["string","null"]},"consensus_sequence_software_version":{"type":["number","null"]},"raw_sequence_data_processing_method":{"type":["string","null"]},"dehosting_method":{"type":["string","null"]},"metrics":{"type":"object","properties":{"breadth_of_coverage":{"type":["string","null"]},"depth_of_coverage":{"type":["string","null"]},"consensus_genome_length":{"type":["integer","null"]},"Ns_per_100kbp":{"type":["number","null"]}}},"reference_genome_accession":{"type":["string","null"]},"bioinformatics_protocol":{"type":["string","null"]}},"propertyNames":{"enum":["consensus_sequence_software_name","consensus_sequence_software_version","raw_sequence_data_processing_method","dehosting_method","metrics","reference_genome_accession","bioinformatics_protocol"]}},"pathogen_diagnostic_testing":{"type":"object","required":["gene_name","diagnostic_pcr_ct_value"],"properties":{"gene_name":{"enum":["E gene (orf4)","M gene (orf5)","N gene (orf9)","Spike gene (orf2)","orf1ab (rep)","orf1a (pp1a)","nsp11","nsp1 ","nsp2 ","nsp3 ","nsp4 ","nsp5 ","nsp6 ","nsp7 ","nsp8 ","nsp9 ","nsp10 ","RdRp gene (nsp12)","hel gene (nsp13)","exoN gene (nsp14)","nsp15","nsp16","orf3a","orf3b","orf6 (ns6)","orf7a","orf7b (ns7b)","orf8 (ns8)","orf9b","orf9c","orf10","orf14","SARS-COV-2 5' UTR","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access"]},"diagnostic_pcr_ct_value":{"type":["number","null"]}},"propertyNames":{"enum":["gene_name","diagnostic_pcr_ct_value"]}},"lineage_analysis":{"type":"object","properties":{"lineage_name":{"type":["string","null"]},"lineage_analysis_software_name":{"type":["string","null"]},"lineage_analysis_software_version":{"type":["string","null"]},"variant_designation":{"enum":["Variant of Concern (VOC)","Variant of Interest (VOI)","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access",null]},"variant_evidence":{"enum":["RT-qPCR","Sequencing","Not Applicable","Missing","Not Collected","Not Provided","Restricted Access",null]},"variant_evidence_details":{"type":["string","null"]}},"propertyNames":{"enum":["lineage_name","lineage_analysis_software_name","lineage_analysis_software_version","variant_designation","variant_evidence","variant_evidence_details"]}}}}
2	1	variantCall	{"type": "object", "required": ["experiment"], "properties": {"experiment": {"type": "object", "required": ["matchedNormalSampleSubmitterId", "variantCallingTool"], "properties": {"variantCallingTool": {"type": "string"}, "matchedNormalSampleSubmitterId": {"type": "string"}}}}}
3	1	sequencingRead	{"type": "object", "required": ["experiment"], "properties": {"experiment": {"type": "object", "required": ["libraryStrategy"], "properties": {"aligned": {"type": ["boolean", "null"]}, "pairedEnd": {"type": ["boolean", "null"]}, "insertSize": {"type": ["integer", "null"]}, "alignmentTool": {"type": ["string", "null"]}, "libraryStrategy": {"enum": ["WGS", "WXS", "RNA-Seq", "ChIP-Seq", "miRNA-Seq", "Bisulfite-Seq", "Validation", "Amplicon", "Other"], "type": "string"}, "referenceGenome": {"type": ["string", "null"]}}}}}
\.


--
-- Name: analysis_schema_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.analysis_schema_id_seq', 2, true);


--
-- Data for Name: donor; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.donor (id, study_id, submitter_id, gender) FROM stdin;
\.


--
-- Data for Name: file; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.file (id, analysis_id, study_id, name, size, md5, type, access) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	Base version	SQL	V1__Base_version.sql	-1608472095	postgres	2019-10-22 19:30:10.105505	493	t
2	1.1	added schema	SQL	V1_1__added_schema.sql	675033696	postgres	2019-10-22 19:30:10.625976	30	t
3	1.2	dynamic schema integration	SPRING_JDBC	db.migration.V1_2__dynamic_schema_integration	\N	postgres	2019-10-22 19:30:10.679764	141	t
4	1.3	post schema integration	SQL	V1_3__post_schema_integration.sql	1429883245	postgres	2019-10-22 19:30:10.885393	13	t
\.


--
-- Data for Name: info; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.info (id, id_type, info) FROM stdin;
\.


--
-- Data for Name: sample; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sample (id, specimen_id, submitter_id, type) FROM stdin;
\.


--
-- Data for Name: sampleset; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sampleset (analysis_id, sample_id) FROM stdin;
\.


--
-- Data for Name: specimen; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.specimen (id, donor_id, submitter_id, class, type) FROM stdin;
\.


--
-- Data for Name: study; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.study (id, name, description, organization) FROM stdin;
COVIDPR	\N	\N	\N
\.


--
-- Data for Name: upload; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.upload (id, study_id, analysis_id, state, errors, payload, created_at, updated_at) FROM stdin;
\.


--
-- Name: analysis_data analysis_data_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_data
    ADD CONSTRAINT analysis_data_pkey PRIMARY KEY (id);


--
-- Name: analysis analysis_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_pkey PRIMARY KEY (id);


--
-- Name: analysis_schema analysis_schema_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_schema
    ADD CONSTRAINT analysis_schema_pkey PRIMARY KEY (id);


--
-- Name: donor donor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.donor
    ADD CONSTRAINT donor_pkey PRIMARY KEY (id);


--
-- Name: file file_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT file_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: sample sample_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT sample_pkey PRIMARY KEY (id);


--
-- Name: specimen specimen_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.specimen
    ADD CONSTRAINT specimen_pkey PRIMARY KEY (id);


--
-- Name: study study_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.study
    ADD CONSTRAINT study_pkey PRIMARY KEY (id);


--
-- Name: upload upload_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.upload
    ADD CONSTRAINT upload_pkey PRIMARY KEY (id);


--
-- Name: analysis_id_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX analysis_id_study_id_uindex ON public.analysis USING btree (id, study_id);


--
-- Name: analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX analysis_id_uindex ON public.analysis USING btree (id);


--
-- Name: analysis_schema_name_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_schema_name_index ON public.analysis_schema USING btree (name);


--
-- Name: analysis_schema_name_version_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_schema_name_version_index ON public.analysis_schema USING btree (name, version);


--
-- Name: analysis_schema_version_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_schema_version_index ON public.analysis_schema USING btree (version);


--
-- Name: analysis_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX analysis_study_id_uindex ON public.analysis USING btree (study_id);


--
-- Name: donor_id_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX donor_id_study_id_uindex ON public.donor USING btree (id, study_id);


--
-- Name: donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX donor_id_uindex ON public.donor USING btree (id);


--
-- Name: donor_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX donor_study_id_uindex ON public.donor USING btree (study_id);


--
-- Name: donor_submitter_id_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX donor_submitter_id_study_id_uindex ON public.donor USING btree (submitter_id, study_id);


--
-- Name: donor_submitter_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX donor_submitter_id_uindex ON public.donor USING btree (submitter_id);


--
-- Name: file_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX file_analysis_id_uindex ON public.file USING btree (analysis_id);


--
-- Name: file_id_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX file_id_analysis_id_uindex ON public.file USING btree (id, analysis_id);


--
-- Name: file_id_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX file_id_index ON public.file USING btree (id);


--
-- Name: file_name_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX file_name_analysis_id_uindex ON public.file USING btree (name, analysis_id);


--
-- Name: file_study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX file_study_id_uindex ON public.file USING btree (study_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: info_id_id_type_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX info_id_id_type_uindex ON public.info USING btree (id, id_type);


--
-- Name: info_id_type_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX info_id_type_uindex ON public.info USING btree (id_type);


--
-- Name: info_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX info_id_uindex ON public.info USING btree (id);


--
-- Name: sample_id_specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX sample_id_specimen_id_uindex ON public.sample USING btree (id, specimen_id);


--
-- Name: sample_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX sample_id_uindex ON public.sample USING btree (id);


--
-- Name: sample_specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sample_specimen_id_uindex ON public.sample USING btree (specimen_id);


--
-- Name: sample_submitter_id_specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX sample_submitter_id_specimen_id_uindex ON public.sample USING btree (submitter_id, specimen_id);


--
-- Name: sample_submitter_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sample_submitter_id_uindex ON public.sample USING btree (submitter_id);


--
-- Name: sampleset_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sampleset_analysis_id_uindex ON public.sampleset USING btree (analysis_id);


--
-- Name: sampleset_sample_id_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sampleset_sample_id_analysis_id_uindex ON public.sampleset USING btree (sample_id, analysis_id);


--
-- Name: sampleset_sample_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX sampleset_sample_id_uindex ON public.sampleset USING btree (sample_id);


--
-- Name: specimen_donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX specimen_donor_id_uindex ON public.specimen USING btree (donor_id);


--
-- Name: specimen_id_donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX specimen_id_donor_id_uindex ON public.specimen USING btree (id, donor_id);


--
-- Name: specimen_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX specimen_id_uindex ON public.specimen USING btree (id);


--
-- Name: specimen_submitter_id_donor_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX specimen_submitter_id_donor_id_uindex ON public.specimen USING btree (submitter_id, donor_id);


--
-- Name: specimen_submitter_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX specimen_submitter_id_uindex ON public.specimen USING btree (submitter_id);


--
-- Name: study_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX study_id_uindex ON public.study USING btree (id);


--
-- Name: upload_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX upload_id_uindex ON public.upload USING btree (id);


--
-- Name: upload_study_id_analysis_id_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX upload_study_id_analysis_id_uindex ON public.upload USING btree (study_id, analysis_id);


--
-- Name: analysis analysis_data_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_data_id_fk FOREIGN KEY (analysis_data_id) REFERENCES public.analysis_data(id);


--
-- Name: analysis analysis_schema_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_schema_id_fk FOREIGN KEY (analysis_schema_id) REFERENCES public.analysis_schema(id);


--
-- Name: analysis analysis_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: donor donor_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.donor
    ADD CONSTRAINT donor_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: file file_analysis_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT file_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES public.analysis(id);


--
-- Name: file file_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT file_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: sample sample_specimen_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT sample_specimen_id_fkey FOREIGN KEY (specimen_id) REFERENCES public.specimen(id);


--
-- Name: sampleset sampleset_analysis_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sampleset
    ADD CONSTRAINT sampleset_analysis_id_fkey FOREIGN KEY (analysis_id) REFERENCES public.analysis(id);


--
-- Name: sampleset sampleset_sample_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sampleset
    ADD CONSTRAINT sampleset_sample_id_fkey FOREIGN KEY (sample_id) REFERENCES public.sample(id);


--
-- Name: specimen specimen_donor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.specimen
    ADD CONSTRAINT specimen_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES public.donor(id);


--
-- Name: upload upload_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.upload
    ADD CONSTRAINT upload_study_id_fkey FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- PostgreSQL database dump complete
--

