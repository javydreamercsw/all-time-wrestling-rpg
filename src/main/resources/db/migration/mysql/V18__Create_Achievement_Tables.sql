CREATE TABLE achievement (
    achievement_id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    icon_url VARCHAR(512),
    xp_value INT NOT NULL,
    CONSTRAINT pk_achievement PRIMARY KEY (achievement_id),
    CONSTRAINT uc_achievement_name UNIQUE (name)
);

CREATE TABLE account_achievement (
    account_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    CONSTRAINT pk_account_achievement PRIMARY KEY (account_id, achievement_id),
    CONSTRAINT fk_account_achievement_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_account_achievement_achievement FOREIGN KEY (achievement_id) REFERENCES achievement (achievement_id)
);
