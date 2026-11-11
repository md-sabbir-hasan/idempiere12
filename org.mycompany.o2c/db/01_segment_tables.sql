SET search_path TO adempiere, public;

DROP TABLE IF EXISTS xx_segmentterritory CASCADE;
DROP TABLE IF EXISTS xx_bpartnersegment CASCADE;
DROP TABLE IF EXISTS xx_segment CASCADE;

CREATE TABLE xx_segment (
  xx_segment_id numeric(10) NOT NULL,
  ad_client_id numeric(10) NOT NULL,
  ad_org_id numeric(10) NOT NULL,
  isactive char(1) DEFAULT 'Y' NOT NULL,
  created timestamp DEFAULT now() NOT NULL,
  createdby numeric(10) NOT NULL,
  updated timestamp DEFAULT now() NOT NULL,
  updatedby numeric(10) NOT NULL,
  value varchar(40) NOT NULL,
  name varchar(120) NOT NULL,
  CONSTRAINT xx_segment_pkey PRIMARY KEY (xx_segment_id)
);

CREATE TABLE xx_segmentterritory (
  xx_segmentterritory_id numeric(10) NOT NULL,
  ad_client_id numeric(10) NOT NULL,
  ad_org_id numeric(10) NOT NULL,
  isactive char(1) DEFAULT 'Y' NOT NULL,
  created timestamp DEFAULT now() NOT NULL,
  createdby numeric(10) NOT NULL,
  updated timestamp DEFAULT now() NOT NULL,
  updatedby numeric(10) NOT NULL,
  xx_segment_id numeric(10) NOT NULL,
  xx_territory_id numeric(10) NOT NULL,
  CONSTRAINT xx_segmentterritory_pkey PRIMARY KEY (xx_segmentterritory_id)
);

CREATE TABLE xx_bpartnersegment (
  xx_bpartnersegment_id numeric(10) NOT NULL,
  ad_client_id numeric(10) NOT NULL,
  ad_org_id numeric(10) NOT NULL,
  isactive char(1) DEFAULT 'Y' NOT NULL,
  created timestamp DEFAULT now() NOT NULL,
  createdby numeric(10) NOT NULL,
  updated timestamp DEFAULT now() NOT NULL,
  updatedby numeric(10) NOT NULL,
  c_bpartner_id numeric(10) NOT NULL,
  xx_segment_id numeric(10) NOT NULL,
  CONSTRAINT xx_bpartnersegment_pkey PRIMARY KEY (xx_bpartnersegment_id)
);

ALTER TABLE c_order ADD COLUMN IF NOT EXISTS xx_segment_id numeric(10);
ALTER TABLE c_order ADD COLUMN IF NOT EXISTS xx_territory_id numeric(10);
ALTER TABLE c_order ADD COLUMN IF NOT EXISTS xx_salestype char(1);
ALTER TABLE c_order ADD COLUMN IF NOT EXISTS xx_issitedelivery char(1) DEFAULT 'N';
ALTER TABLE c_order ADD COLUMN IF NOT EXISTS xx_sitedeliveryaddress varchar(255);