-- ==========================================
-- DATA INITIALIZATION
-- ==========================================

-- Insert default permissions
INSERT INTO permissions (name, description)
VALUES ('FULL_ACCESS', 'Permission to have full access to all features'),
       ('CREATE_SERVER', 'Permission to create a new server'),
       ('DELETE_SERVER', 'Permission to delete a server'),
       ('MANAGE_SERVER', 'Permission to manage server settings'),
       ('CREATE_CHANNEL', 'Permission to create channels in a server'),
       ('DELETE_CHANNEL', 'Permission to delete channels in a server'),
       ('MANAGE_CHANNEL', 'Permission to manage channel settings'),
       ('SEND_MESSAGES', 'Permission to send messages in channels'),
       ('DELETE_MESSAGES', 'Permission to delete messages in channels'),
       ('BAN_USERS', 'Permission to ban users from the server'),
       ('KICK_USERS', 'Permission to kick users from the server');

INSERT INTO roles (name, description)
VALUES ('ADMIN', 'Administrator with full permissions'),
       ('MODERATOR', 'Moderator with limited management permissions'),
       ('USER', 'Regular user with standard permissions');

INSERT INTO role_permissions (role_id, permission_id)
VALUES ((SELECT id FROM roles WHERE name = 'ADMIN'),
        (SELECT id FROM permissions WHERE name = 'FULL_ACCESS'));

-- Sample user & admin account (at least 10 users (1 admin & 9 temp user))
INSERT INTO Users (username, password, kyc_status)
VALUES ('admin@gmail.com', '$2a$10$NL.fF5iJyANZKrvzuCjUT.V7DQrFE5oddrZ1vIouVi07UimJ2tX1y', 'VERIFIED'), -- password: admin123,
       ('user1@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'VERIFIED'), -- password: user1
       ('user2@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED'),
       ('user3@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED'),
       ('user4@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED'),
       ('user5@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED'),
       ('user6@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED'),
       ('user7@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED'),
       ('user8@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED'),
       ('user9@gmail.com', '$2a$10$LQC60YO.ZW1AYMMcKEkxfuaKSjK4rSTAYV1r28eThu8IHfVgrQWI.', 'UNVERIFIED');

INSERT INTO user_profiles (user_id, display_name, date_of_birth, avatar_url, bio)
VALUES ((SELECT id FROM users WHERE username = 'admin@gmail.com'),
        'Admin',
        '1990-01-01',
        'https://example.com/avatars/admin.png',
        'I am the administrator of this platform.'),
       ((SELECT id FROM users WHERE username = 'user1@gmail.com'),
        'User One',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user2@gmail.com'),
        'User Two',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user3@gmail.com'),
        'User Three',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user4@gmail.com'),
        'User Four',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user5@gmail.com'),
        'User Five',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user6@gmail.com'),
        'User Six',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user7@gmail.com'),
        'User Seven',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user8@gmail.com'),
        'User Eight',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.'),
       ((SELECT id FROM users WHERE username = 'user9@gmail.com'),
        'User Nine',
        '1995-05-15',
        'https://example.com/avatars/user1.png',
        'Hello! I am User One.');

INSERT INTO user_roles (user_id, role_id)
VALUES ((SELECT id FROM users WHERE username = 'admin@gmail.com'),
        (SELECT id FROM roles WHERE name = 'ADMIN')),
       ((SELECT id FROM users WHERE username = 'user1@gmail.com'),
        (SELECT id FROM roles WHERE name = 'USER'));