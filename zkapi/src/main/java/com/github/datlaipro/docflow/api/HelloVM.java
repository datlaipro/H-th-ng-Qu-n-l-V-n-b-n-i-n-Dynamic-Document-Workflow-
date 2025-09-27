package com.github.datlaipro.docflow.api;

import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.util.Clients;

public class HelloVM {
    private String name;

    public String getName() { return name; }
    public void setName(String n) { this.name = n; }

    public String getGreeting() {
        return (name == null || name.trim().isEmpty())
                ? "Chào mừng đến ZK 7.0.5"
                : "Hello, " + name + "!";
    }

    @Command @NotifyChange("greeting")
    public void say() {
        // không cần xử lý gì thêm; @NotifyChange sẽ cập nhật label
        Clients.showNotification("Đã cập nhật lời chào!", "info", null, "top_center", 1500);
    }

    @Command
    public void approve() {
        // giả lập nghiệp vụ phê duyệt
        Clients.showNotification("Đã phê duyệt văn bản 🎉", "success", null, "top_center", 1500);
    }

    @Command
    public void forward() {
        // giả lập nghiệp vụ chuyển tiếp
        Clients.showNotification("Đã chuyển tiếp cho người xử lý tiếp theo ➡️", "info", null, "top_center", 1500);
    }

    @Command
    public void reject() {
        // giả lập nghiệp vụ từ chối
        Clients.showNotification("Đã từ chối văn bản ❌", "warning", null, "top_center", 1500);
    }
}
