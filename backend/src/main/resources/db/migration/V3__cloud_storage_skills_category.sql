-- Attachments can live on the server disk (LOCAL) or Cloudinary (CLOUDINARY)
alter table attachments
    add column storage_provider varchar(20) not null default 'LOCAL',
    add column public_url varchar(600),
    add column cloud_public_id varchar(300),
    add column resource_type varchar(20);

-- Technician skills (comma separated) and work-order category enable skill-based dispatch suggestions
alter table users add column skills varchar(100);
alter table work_orders add column category varchar(20) not null default 'GENERAL';
alter table work_orders add constraint ck_wo_category check (category in ('HVAC', 'ELECTRICAL', 'PLUMBING', 'GENERAL'));

-- Tag the demo data (matches nothing if the demo seed was removed)
update users set skills = 'HVAC,ELECTRICAL' where email = 'tech1@meridian.test';
update users set skills = 'HVAC,PLUMBING' where email = 'tech2@meridian.test';
update users set skills = 'ELECTRICAL,PLUMBING' where email = 'tech3@meridian.test';
update work_orders set category = 'HVAC' where id in (4, 5, 6, 9, 10, 13, 17);
update work_orders set category = 'ELECTRICAL' where id in (1, 3, 7, 12, 14, 15);
update work_orders set category = 'PLUMBING' where id in (2, 8, 11, 16);
