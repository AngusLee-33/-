CREATE TABLE IF NOT EXISTS `users` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `real_name` VARCHAR(50),
    `phone` VARCHAR(20) UNIQUE,
    `email` VARCHAR(100) UNIQUE,
    `role` VARCHAR(20) NOT NULL DEFAULT 'student',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active',
    `id_card` VARCHAR(18),
    `avatar` VARCHAR(255),
    `create_time` DATETIME NOT NULL,
    `update_time` DATETIME
);

CREATE TABLE IF NOT EXISTS `carpool` (
    `carpool_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `departure` VARCHAR(100) NOT NULL,
    `destination` VARCHAR(100) NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `seats` INT NOT NULL,
    `price` DECIMAL(10,2) NOT NULL,
    `description` TEXT,
    `images` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `create_time` DATETIME NOT NULL,
    `update_time` DATETIME,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `carpool_apply` (
    `apply_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `carpool_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `create_time` DATETIME NOT NULL,
    FOREIGN KEY (`carpool_id`) REFERENCES `carpool`(`carpool_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `part_time` (
    `part_time_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `merchant_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT NOT NULL,
    `salary_min` DECIMAL(10,2) NOT NULL,
    `salary_max` DECIMAL(10,2) NOT NULL,
    `recruit_count` INT NOT NULL DEFAULT 1,
    `work_time` VARCHAR(200) NOT NULL,
    `requirements` TEXT,
    `location` VARCHAR(100),
    `images` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `create_time` DATETIME NOT NULL,
    `update_time` DATETIME,
    FOREIGN KEY (`merchant_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `part_time_apply` (
    `apply_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `part_time_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `resume` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `create_time` DATETIME NOT NULL,
    FOREIGN KEY (`part_time_id`) REFERENCES `part_time`(`part_time_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `secondhand` (
    `secondhand_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `price` DECIMAL(10,2) NOT NULL,
    `category` VARCHAR(50) NOT NULL,
    `images` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `create_time` DATETIME NOT NULL,
    `update_time` DATETIME,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `lost_found` (
    `lost_found_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT NOT NULL,
    `location` VARCHAR(120) NOT NULL,
    `event_time` DATETIME,
    `contact` VARCHAR(100) NOT NULL,
    `images` TEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'open',
    `create_time` DATETIME NOT NULL,
    `update_time` DATETIME,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `orders` (
    `order_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `type` VARCHAR(20) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL,
    `amount` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'created',
    `pay_time` DATETIME,
    `complete_time` DATETIME,
    `create_time` DATETIME NOT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `review` (
    `review_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `type` VARCHAR(20) NOT NULL,
    `order_id` BIGINT NOT NULL,
    `reviewer_id` BIGINT NOT NULL,
    `reviewee_id` BIGINT NOT NULL,
    `score` INT NOT NULL,
    `content` TEXT,
    `create_time` DATETIME NOT NULL,
    FOREIGN KEY (`order_id`) REFERENCES `orders`(`order_id`),
    FOREIGN KEY (`reviewer_id`) REFERENCES `users`(`user_id`),
    FOREIGN KEY (`reviewee_id`) REFERENCES `users`(`user_id`)
);

CREATE TABLE IF NOT EXISTS `chat_message` (
    `message_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` BIGINT NOT NULL,
    `receiver_id` BIGINT NOT NULL,
    `target_type` VARCHAR(30) NOT NULL,
    `target_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `read_flag` BOOLEAN NOT NULL DEFAULT FALSE,
    `create_time` DATETIME NOT NULL,
    FOREIGN KEY (`sender_id`) REFERENCES `users`(`user_id`),
    FOREIGN KEY (`receiver_id`) REFERENCES `users`(`user_id`)
);
