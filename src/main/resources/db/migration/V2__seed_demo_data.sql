-- Demo seed data. All seeded users share the password: Password@123

insert into customers (id,name,contact_name,contact_email,contact_phone,address) values
(1,'Northgate Plaza Management','Priya Nair','priya@northgate.test','555-0101','12 Northgate Way'),
(2,'Harbor Medical Group','James Okafor','james@harbormed.test','555-0102','88 Harbor Street'),
(3,'Westfield Logistics','Sofia Alvarez','sofia@westfield.test','555-0103','400 Westfield Industrial Park');

insert into sites (id,customer_id,name,address_line,city,state,postal_code,country) values
(1,1,'Northgate Tower A','12 Northgate Way','Bhopal','MP','462001','India'),
(2,1,'Northgate Retail Podium','14 Northgate Way','Bhopal','MP','462001','India'),
(3,2,'Harbor Main Hospital','88 Harbor Street','Indore','MP','452001','India'),
(4,2,'Harbor Outpatient Clinic','9 Lake Road','Indore','MP','452010','India'),
(5,3,'Westfield Warehouse 4','400 Westfield Industrial Park','Bhopal','MP','462023','India');

insert into users (id,name,email,password_hash,phone,role,customer_id) values
(1,'Maya Rao','manager@meridian.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0201','MANAGER',NULL),
(2,'Daniel Osei','dispatcher@meridian.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0202','DISPATCHER',NULL),
(3,'Arjun Mehta','tech1@meridian.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0203','TECHNICIAN',NULL),
(4,'Lena Fischer','tech2@meridian.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0204','TECHNICIAN',NULL),
(5,'Carlos Ruiz','tech3@meridian.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0205','TECHNICIAN',NULL),
(6,'Priya Nair','customer1@northgate.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0101','CUSTOMER',1),
(7,'James Okafor','customer2@harbormed.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0102','CUSTOMER',2),
(8,'Sofia Alvarez','customer3@westfield.test','$2a$10$jlrplArjsloTLz5Eo8v3hOMOJONUnNr2t48gVYpncRhcYD9.koquC','555-0103','CUSTOMER',3);

insert into parts (id,sku,name,unit_cost,stock_qty) values
(1,'HVAC-FLT-2025','Pleated air filter 20x25',12.50,80),
(2,'CAP-45-5','Run capacitor 45/5 µF',18.90,30),
(3,'CNT-24V','Contactor 24V 30A',34.00,15),
(4,'REF-R410A','Refrigerant R410A (1 lb)',28.50,40),
(5,'PVC-CPL-2','PVC coupling 2 in',3.20,120),
(6,'PIPE-CU-34','Copper pipe 3/4 in (3 m)',21.00,25),
(7,'BRK-20A','Circuit breaker 20A',14.75,50),
(8,'LED-PNL-600','LED panel 600x600',39.00,20),
(9,'THERM-PRG','Programmable thermostat',58.00,12),
(10,'VLV-BALL-12','Ball valve 1/2 in',16.40,35),
(11,'WIRE-12AWG','Wire 12 AWG (30 m)',31.00,18),
(12,'PUMP-SUMP','Sump pump 0.5 HP',142.00,4),
(13,'CMP-SCROLL','Scroll compressor 3-ton',640.00,1);

insert into work_orders (id,code,title,description,status,priority,customer_id,site_id,assigned_to_id,created_by_id,scheduled_at,started_at,completed_at,closed_at,sla_due_at,sla_breached,sla_breached_at,sla_risk_notified,created_at,updated_at) values
(1,'WO-000001','Flickering lights in lobby','Lobby ceiling lights flicker intermittently since this morning.','NEW','MEDIUM',1,1,NULL,6,NULL,NULL,NULL,NULL,(utc_timestamp(6) - interval 120 minute + interval 24 hour),0,NULL,0,(utc_timestamp(6) - interval 120 minute),(utc_timestamp(6) - interval 120 minute)),
(2,'WO-000002','Water leak above server room','Water dripping through ceiling tiles above the main server room.','NEW','URGENT',2,3,NULL,7,NULL,NULL,NULL,NULL,(utc_timestamp(6) - interval 192 minute + interval 4 hour),0,NULL,1,(utc_timestamp(6) - interval 192 minute),(utc_timestamp(6) - interval 192 minute)),
(3,'WO-000003','Replace burnt-out exterior bulbs','Six exterior bulbs along the east wall need replacing.','NEW','LOW',1,2,NULL,2,NULL,NULL,NULL,NULL,(utc_timestamp(6) - interval 1200 minute + interval 72 hour),0,NULL,0,(utc_timestamp(6) - interval 1200 minute),(utc_timestamp(6) - interval 1200 minute)),
(4,'WO-000004','Rooftop unit not cooling','RTU-2 is running but supply air is warm.','ASSIGNED','HIGH',1,1,3,2,NULL,NULL,NULL,NULL,(utc_timestamp(6) - interval 180 minute + interval 8 hour),0,NULL,0,(utc_timestamp(6) - interval 180 minute),(utc_timestamp(6) - interval 180 minute)),
(5,'WO-000005','Thermostat unresponsive on 2nd floor','Display is blank, floor is overheating.','ASSIGNED','MEDIUM',2,4,4,7,NULL,NULL,NULL,NULL,(utc_timestamp(6) - interval 360 minute + interval 24 hour),0,NULL,0,(utc_timestamp(6) - interval 360 minute),(utc_timestamp(6) - interval 360 minute)),
(6,'WO-000006','Chiller trip in ICU wing','Chiller 1 tripped twice overnight. Critical area.','IN_PROGRESS','URGENT',2,3,3,2,NULL,(utc_timestamp(6) - interval 90 minute),NULL,NULL,(utc_timestamp(6) - interval 150 minute + interval 4 hour),0,NULL,0,(utc_timestamp(6) - interval 150 minute),(utc_timestamp(6) - interval 90 minute)),
(7,'WO-000007','Generator panel breaker faulty','Breaker will not hold on backup generator panel.','IN_PROGRESS','HIGH',3,5,5,2,NULL,(utc_timestamp(6) - interval 480 minute),NULL,NULL,(utc_timestamp(6) - interval 600 minute + interval 8 hour),1,(utc_timestamp(6) - interval 120 minute),1,(utc_timestamp(6) - interval 600 minute),(utc_timestamp(6) - interval 480 minute)),
(8,'WO-000008','Clogged condensate drain','Condensate drain backing up in retail unit 6.','IN_PROGRESS','MEDIUM',1,2,4,6,NULL,(utc_timestamp(6) - interval 300 minute),NULL,NULL,(utc_timestamp(6) - interval 720 minute + interval 24 hour),0,NULL,0,(utc_timestamp(6) - interval 720 minute),(utc_timestamp(6) - interval 300 minute)),
(9,'WO-000009','Compressor failure, awaiting part','Compressor seized. Replacement on order.','ON_HOLD','HIGH',2,4,4,2,NULL,(utc_timestamp(6) - interval 1680 minute),NULL,NULL,(utc_timestamp(6) - interval 1800 minute + interval 8 hour),1,(utc_timestamp(6) - interval 1320 minute),1,(utc_timestamp(6) - interval 1800 minute),(utc_timestamp(6) - interval 1680 minute)),
(10,'WO-000010','Quarterly air filter replacement','Scheduled quarterly filter change, all floors.','COMPLETED','MEDIUM',1,1,3,2,NULL,(utc_timestamp(6) - interval 2280 minute),(utc_timestamp(6) - interval 1800 minute),NULL,(utc_timestamp(6) - interval 2400 minute + interval 24 hour),0,NULL,0,(utc_timestamp(6) - interval 2400 minute),(utc_timestamp(6) - interval 1800 minute)),
(11,'WO-000011','Pipe burst in staff restroom','Pipe burst behind sink, flooding the corridor.','COMPLETED','HIGH',3,5,5,8,NULL,(utc_timestamp(6) - interval 1140 minute),(utc_timestamp(6) - interval 360 minute),NULL,(utc_timestamp(6) - interval 1200 minute + interval 8 hour),1,(utc_timestamp(6) - interval 720 minute),1,(utc_timestamp(6) - interval 1200 minute),(utc_timestamp(6) - interval 360 minute)),
(12,'WO-000012','Exit sign battery replacement','Two exit signs failing battery test.','CLOSED','LOW',1,2,3,2,NULL,(utc_timestamp(6) - interval 5880 minute),(utc_timestamp(6) - interval 5400 minute),(utc_timestamp(6) - interval 4800 minute),(utc_timestamp(6) - interval 6000 minute + interval 72 hour),0,NULL,0,(utc_timestamp(6) - interval 6000 minute),(utc_timestamp(6) - interval 4800 minute)),
(13,'WO-000013','Boiler pressure alarm','Pressure alarm on boiler 2.','CLOSED','HIGH',2,3,4,7,NULL,(utc_timestamp(6) - interval 7680 minute),(utc_timestamp(6) - interval 7560 minute),(utc_timestamp(6) - interval 7200 minute),(utc_timestamp(6) - interval 7800 minute + interval 8 hour),0,NULL,0,(utc_timestamp(6) - interval 7800 minute),(utc_timestamp(6) - interval 7200 minute)),
(14,'WO-000014','Corridor lighting upgrade','Replace corridor fluorescents with LED panels.','CLOSED','MEDIUM',1,1,3,2,NULL,(utc_timestamp(6) - interval 3480 minute),(utc_timestamp(6) - interval 3000 minute),(utc_timestamp(6) - interval 2400 minute),(utc_timestamp(6) - interval 3600 minute + interval 24 hour),0,NULL,0,(utc_timestamp(6) - interval 3600 minute),(utc_timestamp(6) - interval 2400 minute)),
(15,'WO-000015','Dock door sensor fault','Dock door 3 sensor stuck closed.','CLOSED','MEDIUM',3,5,5,8,NULL,(utc_timestamp(6) - interval 8880 minute),(utc_timestamp(6) - interval 6000 minute),(utc_timestamp(6) - interval 5760 minute),(utc_timestamp(6) - interval 9000 minute + interval 24 hour),1,(utc_timestamp(6) - interval 7560 minute),1,(utc_timestamp(6) - interval 9000 minute),(utc_timestamp(6) - interval 5760 minute)),
(16,'WO-000016','Blocked drain in basement','Basement floor drain blocked.','CLOSED','MEDIUM',2,4,4,7,NULL,(utc_timestamp(6) - interval 11880 minute),(utc_timestamp(6) - interval 11100 minute),(utc_timestamp(6) - interval 10200 minute),(utc_timestamp(6) - interval 12000 minute + interval 24 hour),0,NULL,0,(utc_timestamp(6) - interval 12000 minute),(utc_timestamp(6) - interval 10200 minute)),
(17,'WO-000017','AC noise in server closet','Grinding noise from closet AC.','CLOSED','HIGH',1,1,3,6,NULL,(utc_timestamp(6) - interval 14280 minute),(utc_timestamp(6) - interval 14160 minute),(utc_timestamp(6) - interval 13200 minute),(utc_timestamp(6) - interval 14400 minute + interval 8 hour),0,NULL,0,(utc_timestamp(6) - interval 14400 minute),(utc_timestamp(6) - interval 13200 minute)),
(18,'WO-000018','Cancelled: duplicate request','Duplicate of an existing ticket.','CANCELLED','LOW',1,2,NULL,6,NULL,NULL,NULL,NULL,(utc_timestamp(6) - interval 3000 minute + interval 72 hour),0,NULL,0,(utc_timestamp(6) - interval 3000 minute),(utc_timestamp(6) - interval 3000 minute));

insert into work_order_status_history (work_order_id,from_status,to_status,changed_by_id,changed_at,note) values
(1,NULL,'NEW',6,(utc_timestamp(6) - interval 120 minute),'Request raised by customer'),
(2,NULL,'NEW',7,(utc_timestamp(6) - interval 192 minute),'Request raised by customer'),
(3,NULL,'NEW',2,(utc_timestamp(6) - interval 1200 minute),'Work order created'),
(4,NULL,'NEW',2,(utc_timestamp(6) - interval 180 minute),'Work order created'),
(4,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 168 minute),'Assigned to Arjun Mehta'),
(5,NULL,'NEW',7,(utc_timestamp(6) - interval 360 minute),'Request raised by customer'),
(5,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 348 minute),'Assigned to Lena Fischer'),
(6,NULL,'NEW',2,(utc_timestamp(6) - interval 150 minute),'Work order created'),
(6,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 138 minute),'Assigned to Arjun Mehta'),
(6,'ASSIGNED','IN_PROGRESS',3,(utc_timestamp(6) - interval 90 minute),'Started work'),
(7,NULL,'NEW',2,(utc_timestamp(6) - interval 600 minute),'Work order created'),
(7,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 588 minute),'Assigned to Carlos Ruiz'),
(7,'ASSIGNED','IN_PROGRESS',5,(utc_timestamp(6) - interval 480 minute),'Started work'),
(8,NULL,'NEW',6,(utc_timestamp(6) - interval 720 minute),'Request raised by customer'),
(8,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 708 minute),'Assigned to Lena Fischer'),
(8,'ASSIGNED','IN_PROGRESS',4,(utc_timestamp(6) - interval 300 minute),'Started work'),
(9,NULL,'NEW',2,(utc_timestamp(6) - interval 1800 minute),'Work order created'),
(9,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 1788 minute),'Assigned to Lena Fischer'),
(9,'ASSIGNED','IN_PROGRESS',4,(utc_timestamp(6) - interval 1680 minute),'Started work'),
(9,'IN_PROGRESS','ON_HOLD',4,(utc_timestamp(6) - interval 1620 minute),'Waiting on compressor delivery'),
(10,NULL,'NEW',2,(utc_timestamp(6) - interval 2400 minute),'Work order created'),
(10,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 2388 minute),'Assigned to Arjun Mehta'),
(10,'ASSIGNED','IN_PROGRESS',3,(utc_timestamp(6) - interval 2280 minute),'Started work'),
(10,'IN_PROGRESS','COMPLETED',3,(utc_timestamp(6) - interval 1800 minute),'Work finished'),
(11,NULL,'NEW',8,(utc_timestamp(6) - interval 1200 minute),'Request raised by customer'),
(11,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 1188 minute),'Assigned to Carlos Ruiz'),
(11,'ASSIGNED','IN_PROGRESS',5,(utc_timestamp(6) - interval 1140 minute),'Started work'),
(11,'IN_PROGRESS','COMPLETED',5,(utc_timestamp(6) - interval 360 minute),'Work finished'),
(12,NULL,'NEW',2,(utc_timestamp(6) - interval 6000 minute),'Work order created'),
(12,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 5988 minute),'Assigned to Arjun Mehta'),
(12,'ASSIGNED','IN_PROGRESS',3,(utc_timestamp(6) - interval 5880 minute),'Started work'),
(12,'IN_PROGRESS','COMPLETED',3,(utc_timestamp(6) - interval 5400 minute),'Work finished'),
(12,'COMPLETED','CLOSED',1,(utc_timestamp(6) - interval 4800 minute),'Signed off'),
(13,NULL,'NEW',7,(utc_timestamp(6) - interval 7800 minute),'Request raised by customer'),
(13,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 7788 minute),'Assigned to Lena Fischer'),
(13,'ASSIGNED','IN_PROGRESS',4,(utc_timestamp(6) - interval 7680 minute),'Started work'),
(13,'IN_PROGRESS','COMPLETED',4,(utc_timestamp(6) - interval 7560 minute),'Work finished'),
(13,'COMPLETED','CLOSED',1,(utc_timestamp(6) - interval 7200 minute),'Signed off'),
(14,NULL,'NEW',2,(utc_timestamp(6) - interval 3600 minute),'Work order created'),
(14,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 3588 minute),'Assigned to Arjun Mehta'),
(14,'ASSIGNED','IN_PROGRESS',3,(utc_timestamp(6) - interval 3480 minute),'Started work'),
(14,'IN_PROGRESS','COMPLETED',3,(utc_timestamp(6) - interval 3000 minute),'Work finished'),
(14,'COMPLETED','CLOSED',1,(utc_timestamp(6) - interval 2400 minute),'Signed off'),
(15,NULL,'NEW',8,(utc_timestamp(6) - interval 9000 minute),'Request raised by customer'),
(15,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 8988 minute),'Assigned to Carlos Ruiz'),
(15,'ASSIGNED','IN_PROGRESS',5,(utc_timestamp(6) - interval 8880 minute),'Started work'),
(15,'IN_PROGRESS','COMPLETED',5,(utc_timestamp(6) - interval 6000 minute),'Work finished'),
(15,'COMPLETED','CLOSED',1,(utc_timestamp(6) - interval 5760 minute),'Signed off'),
(16,NULL,'NEW',7,(utc_timestamp(6) - interval 12000 minute),'Request raised by customer'),
(16,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 11988 minute),'Assigned to Lena Fischer'),
(16,'ASSIGNED','IN_PROGRESS',4,(utc_timestamp(6) - interval 11880 minute),'Started work'),
(16,'IN_PROGRESS','COMPLETED',4,(utc_timestamp(6) - interval 11100 minute),'Work finished'),
(16,'COMPLETED','CLOSED',1,(utc_timestamp(6) - interval 10200 minute),'Signed off'),
(17,NULL,'NEW',6,(utc_timestamp(6) - interval 14400 minute),'Request raised by customer'),
(17,'NEW','ASSIGNED',2,(utc_timestamp(6) - interval 14388 minute),'Assigned to Arjun Mehta'),
(17,'ASSIGNED','IN_PROGRESS',3,(utc_timestamp(6) - interval 14280 minute),'Started work'),
(17,'IN_PROGRESS','COMPLETED',3,(utc_timestamp(6) - interval 14160 minute),'Work finished'),
(17,'COMPLETED','CLOSED',1,(utc_timestamp(6) - interval 13200 minute),'Signed off'),
(18,NULL,'NEW',6,(utc_timestamp(6) - interval 3000 minute),'Request raised by customer'),
(18,'NEW','CANCELLED',2,(utc_timestamp(6) - interval 2700 minute),'Duplicate of an existing ticket');

insert into part_usages (work_order_id,part_id,qty_used,unit_cost,used_by_id,used_at) select u.w,p.id,u.q,p.unit_cost,u.t,u.at from (
select 6 w,3 pid,1 q,3 t,(utc_timestamp(6) - interval 60 minute) at
union all
select 7 w,7 pid,1 q,5 t,(utc_timestamp(6) - interval 360 minute) at
union all
select 10 w,1 pid,12 q,3 t,(utc_timestamp(6) - interval 1920 minute) at
union all
select 11 w,6 pid,1 q,5 t,(utc_timestamp(6) - interval 600 minute) at
union all
select 11 w,5 pid,2 q,5 t,(utc_timestamp(6) - interval 600 minute) at
union all
select 14 w,8 pid,6 q,3 t,(utc_timestamp(6) - interval 3300 minute) at
union all
select 12 w,7 pid,1 q,3 t,(utc_timestamp(6) - interval 5700 minute) at
union all
select 15 w,10 pid,1 q,5 t,(utc_timestamp(6) - interval 7200 minute) at
union all
select 17 w,2 pid,1 q,3 t,(utc_timestamp(6) - interval 14220 minute) at
union all
select 8 w,5 pid,1 q,4 t,(utc_timestamp(6) - interval 240 minute) at) u join parts p on p.id=u.pid;

insert into time_logs (work_order_id,technician_id,minutes,note,logged_at) values
(6,3,45,'Diagnosed tripped overload, replacing contactor',(utc_timestamp(6) - interval 72 minute)),
(7,5,90,'Traced fault to breaker, replacing',(utc_timestamp(6) - interval 360 minute)),
(8,4,30,'Cleared drain line',(utc_timestamp(6) - interval 240 minute)),
(10,3,180,'All floors done',(utc_timestamp(6) - interval 1920 minute)),
(11,5,120,'Repaired pipe and dried corridor',(utc_timestamp(6) - interval 480 minute)),
(12,3,40,NULL,(utc_timestamp(6) - interval 5520 minute)),
(13,4,75,NULL,(utc_timestamp(6) - interval 7620 minute)),
(14,3,240,'LED panels fitted',(utc_timestamp(6) - interval 3120 minute)),
(15,5,60,NULL,(utc_timestamp(6) - interval 7200 minute)),
(16,4,50,NULL,(utc_timestamp(6) - interval 11400 minute)),
(17,3,95,'Replaced capacitor',(utc_timestamp(6) - interval 14220 minute));

update parts p set stock_qty = stock_qty - coalesce((select sum(qty_used) from part_usages u where u.part_id=p.id),0);
