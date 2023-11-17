insert into _roles values
(1, 'Allow user to manage people', 'USER_MANAGEMENT', 'User Management' );
insert into _roles values
(2, 'Allow user to manage classes', 'CLASS_MANAGEMENT', 'Class Management');

insert into _groups values
(1, 'Admin group', 'Admin');
insert into _groups values
(2, 'Teacher group', 'Teacher');
insert into _groups values
(3, 'Student group', 'Student');

insert into _group_roles values
(1, 1, 1);
insert into _group_roles values
(2, 1, 2);