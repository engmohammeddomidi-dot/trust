package com.trust.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * يعيد توجيه مسارات React Router (مثل /decisions أو /admin/organizations) إلى index.html
 * عند النشر المدمج (الواجهة كملفات ثابتة داخل نفس تطبيق Spring Boot)، بحيث يعمل تحديث
 * الصفحة أو الدخول المباشر برابط عميق بدل الحصول على 404.
 *
 * لا يتعارض مع /api/** لأن أنماط المسار هنا محصورة بمقطع واحد بلا نقطة (لا تعبر "/")،
 * فلا تطابق مسارات API متعددة المقاطع، ولا تطابق ملفات الأصول الثابتة (تحتوي نقطة، مثل .js/.css).
 */
@Controller
public class SpaFallbackController {

    @RequestMapping("/{path:[^\\.]*}")
    public String forwardTopLevel() {
        return "forward:/index.html";
    }

    @RequestMapping("/admin/{path:[^\\.]*}")
    public String forwardAdmin() {
        return "forward:/index.html";
    }
}
