package com.simpleledger.app;

public class Project {
    private long id;
    private String name;
    private String description;
    private long createTime;

    public Project() {}

    public Project(String name, String description) {
        this.name = name;
        this.description = description;
        this.createTime = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
