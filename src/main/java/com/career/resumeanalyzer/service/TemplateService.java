package com.career.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TemplateService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArrayNode getAvailableTemplates() {
        ArrayNode templates = objectMapper.createArrayNode();

        // 1. ATS Friendly Resume
        ObjectNode t1 = objectMapper.createObjectNode();
        t1.put("id", "ats");
        t1.put("name", "ATS Friendly Resume");
        t1.put("badge", "ATS Optimized");
        t1.put("description", "Simple single-column layout optimized for Applicant Tracking Systems with clean section hierarchy.");
        t1.put("accentColor", "#2563eb");
        t1.put("category", "Standard");
        templates.add(t1);

        // 2. Professional Resume
        ObjectNode t2 = objectMapper.createObjectNode();
        t2.put("id", "professional");
        t2.put("name", "Professional Corporate");
        t2.put("badge", "Executive");
        t2.put("description", "Clean corporate design with strong typography, header banner, and structured section dividers.");
        t2.put("accentColor", "#1e293b");
        t2.put("category", "Corporate");
        templates.add(t2);

        // 3. Modern Resume
        ObjectNode t3 = objectMapper.createObjectNode();
        t3.put("id", "modern");
        t3.put("name", "Modern Polished");
        t3.put("badge", "Popular");
        t3.put("description", "Modern visual hierarchy with subtle design accents, skills tags, and side highlight bars.");
        t3.put("accentColor", "#6366f1");
        t3.put("category", "Modern");
        templates.add(t3);

        // 4. Minimal Resume
        ObjectNode t4 = objectMapper.createObjectNode();
        t4.put("id", "minimal");
        t4.put("name", "Minimalist Clean");
        t4.put("badge", "Clean & Simple");
        t4.put("description", "Ultra-clean layout with generous whitespace, subtle lines, and focus on pure content readability.");
        t4.put("accentColor", "#475569");
        t4.put("category", "Minimal");
        templates.add(t4);

        // 5. Creative Resume
        ObjectNode t5 = objectMapper.createObjectNode();
        t5.put("id", "creative");
        t5.put("name", "Creative Portfolio");
        t5.put("badge", "Visual & Tech");
        t5.put("description", "Vibrant dual-column layout with dark sidebar for skills and contact, perfect for design and tech roles.");
        t5.put("accentColor", "#8b5cf6");
        t5.put("category", "Creative");
        templates.add(t5);

        return templates;
    }

    public ObjectNode renderTemplate(String templateId, JsonNode resumeData) {
        ObjectNode result = objectMapper.createObjectNode();
        String activeTemplate = (templateId != null && !templateId.isEmpty()) ? templateId.toLowerCase() : "ats";

        result.put("templateId", activeTemplate);
        result.put("renderedAt", System.currentTimeMillis());

        // Extract profile fields gracefully
        String name = getField(resumeData, "name", "fullName", "Candidate Name");
        String email = getField(resumeData, "email", "email", "");
        String phone = getField(resumeData, "phone", "phone", "");
        String linkedin = getField(resumeData, "linkedin", "linkedinUrl", "");
        String github = getField(resumeData, "github", "githubUrl", "");
        String portfolio = getField(resumeData, "portfolio", "portfolioUrl", "");
        String summary = getField(resumeData, "summary", "summary", "");

        List<String> skillsList = getArrayOrCsv(resumeData, "skills");
        List<JsonNode> educationList = getJsonArray(resumeData, "education");
        List<JsonNode> experienceList = getJsonArray(resumeData, "experience");
        List<JsonNode> projectsList = getJsonArray(resumeData, "projects");

        // Generate template HTML
        String html = "";
        switch (activeTemplate) {
            case "professional":
                html = generateProfessionalHtml(name, email, phone, linkedin, github, portfolio, summary, skillsList, educationList, experienceList, projectsList);
                break;
            case "modern":
                html = generateModernHtml(name, email, phone, linkedin, github, portfolio, summary, skillsList, educationList, experienceList, projectsList);
                break;
            case "minimal":
                html = generateMinimalHtml(name, email, phone, linkedin, github, portfolio, summary, skillsList, educationList, experienceList, projectsList);
                break;
            case "creative":
                html = generateCreativeHtml(name, email, phone, linkedin, github, portfolio, summary, skillsList, educationList, experienceList, projectsList);
                break;
            case "ats":
            default:
                html = generateAtsHtml(name, email, phone, linkedin, github, portfolio, summary, skillsList, educationList, experienceList, projectsList);
                break;
        }

        result.put("html", html);
        result.put("name", name);

        return result;
    }

    private String getField(JsonNode node, String key1, String key2, String defaultVal) {
        if (node == null) return defaultVal;
        if (node.has(key1) && !node.get(key1).isNull() && !node.get(key1).asText().trim().isEmpty()) {
            return node.get(key1).asText().trim();
        }
        if (node.has(key2) && !node.get(key2).isNull() && !node.get(key2).asText().trim().isEmpty()) {
            return node.get(key2).asText().trim();
        }
        return defaultVal;
    }

    private List<String> getArrayOrCsv(JsonNode node, String key) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.has(key)) return list;
        JsonNode target = node.get(key);
        if (target.isArray()) {
            for (JsonNode item : target) {
                if (item.isTextual()) list.add(item.asText());
                else if (item.has("name")) list.add(item.get("name").asText());
            }
        } else if (target.isTextual()) {
            String val = target.asText();
            if (val.startsWith("[")) {
                try {
                    JsonNode parsed = objectMapper.readTree(val);
                    if (parsed.isArray()) {
                        for (JsonNode item : parsed) {
                            if (item.isTextual()) list.add(item.asText());
                            else if (item.has("name")) list.add(item.get("name").asText());
                        }
                    }
                } catch (Exception ignored) {}
            } else {
                String[] parts = val.split("[,;]");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) list.add(p.trim());
                }
            }
        }
        return list;
    }

    private List<JsonNode> getJsonArray(JsonNode node, String key) {
        List<JsonNode> list = new ArrayList<>();
        if (node == null || !node.has(key)) return list;
        JsonNode target = node.get(key);
        if (target.isArray()) {
            for (JsonNode item : target) list.add(item);
        } else if (target.isTextual()) {
            String val = target.asText();
            if (val.startsWith("[")) {
                try {
                    JsonNode parsed = objectMapper.readTree(val);
                    if (parsed.isArray()) {
                        for (JsonNode item : parsed) list.add(item);
                    }
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    // Template 1: ATS Friendly (Clean single column)
    private String generateAtsHtml(String name, String email, String phone, String linkedin, String github, String portfolio,
                                    String summary, List<String> skills, List<JsonNode> edus, List<JsonNode> exps, List<JsonNode> projs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='tmpl-ats-container' style='font-family: Arial, sans-serif; color: #111; padding: 30px; max-width: 800px; margin: 0 auto; background: #fff; line-height: 1.5;'>");
        
        // Header
        sb.append("<div style='text-align: center; border-bottom: 2px solid #333; padding-bottom: 15px; margin-bottom: 20px;'>");
        sb.append("<h1 style='font-size: 26px; text-transform: uppercase; margin: 0 0 8px 0; font-weight: bold; letter-spacing: 1px;'>").append(escape(name)).append("</h1>");
        sb.append("<div style='font-size: 13px; color: #444;'>");
        List<String> contacts = new ArrayList<>();
        if (!email.isEmpty()) contacts.add(email);
        if (!phone.isEmpty()) contacts.add(phone);
        if (!linkedin.isEmpty()) contacts.add(linkedin);
        if (!github.isEmpty()) contacts.add(github);
        if (!portfolio.isEmpty()) contacts.add(portfolio);
        sb.append(String.join(" | ", contacts));
        sb.append("</div></div>");

        // Summary
        if (!summary.isEmpty()) {
            sb.append("<div style='margin-bottom: 20px;'>");
            sb.append("<h2 style='font-size: 16px; text-transform: uppercase; border-bottom: 1px solid #ccc; padding-bottom: 4px; margin-bottom: 8px;'>Professional Summary</h2>");
            sb.append("<p style='font-size: 13px; margin: 0;'>").append(escape(summary)).append("</p>");
            sb.append("</div>");
        }

        // Skills
        if (!skills.isEmpty()) {
            sb.append("<div style='margin-bottom: 20px;'>");
            sb.append("<h2 style='font-size: 16px; text-transform: uppercase; border-bottom: 1px solid #ccc; padding-bottom: 4px; margin-bottom: 8px;'>Technical Skills</h2>");
            sb.append("<p style='font-size: 13px; margin: 0;'>").append(String.join(", ", skills)).append("</p>");
            sb.append("</div>");
        }

        // Experience
        if (!exps.isEmpty()) {
            sb.append("<div style='margin-bottom: 20px;'>");
            sb.append("<h2 style='font-size: 16px; text-transform: uppercase; border-bottom: 1px solid #ccc; padding-bottom: 4px; margin-bottom: 12px;'>Work Experience</h2>");
            for (JsonNode exp : exps) {
                String role = getJsonString(exp, "title", "role", "Position");
                String company = getJsonString(exp, "company", "organization", "");
                String duration = getJsonString(exp, "duration", "dates", "");
                String desc = getJsonString(exp, "description", "details", "");

                sb.append("<div style='margin-bottom: 12px;'>");
                sb.append("<div style='display: flex; justify-content: space-between; font-weight: bold; font-size: 14px;'>");
                sb.append("<span>").append(escape(role)).append(company.isEmpty() ? "" : " - " + escape(company)).append("</span>");
                sb.append("<span>").append(escape(duration)).append("</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) {
                    sb.append("<p style='font-size: 13px; margin: 4px 0 0 0; color: #333;'>").append(escape(desc)).append("</p>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        // Projects
        if (!projs.isEmpty()) {
            sb.append("<div style='margin-bottom: 20px;'>");
            sb.append("<h2 style='font-size: 16px; text-transform: uppercase; border-bottom: 1px solid #ccc; padding-bottom: 4px; margin-bottom: 12px;'>Projects</h2>");
            for (JsonNode proj : projs) {
                String title = getJsonString(proj, "name", "title", "Project");
                String tech = getJsonString(proj, "technologies", "tech", "");
                String desc = getJsonString(proj, "description", "details", "");

                sb.append("<div style='margin-bottom: 10px;'>");
                sb.append("<div style='font-weight: bold; font-size: 14px;'>").append(escape(title));
                if (!tech.isEmpty()) sb.append("<span style='font-weight: normal; font-size: 12px; color: #666;'> (").append(escape(tech)).append(")</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) sb.append("<p style='font-size: 13px; margin: 2px 0 0 0;'>").append(escape(desc)).append("</p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        // Education
        if (!edus.isEmpty()) {
            sb.append("<div style='margin-bottom: 20px;'>");
            sb.append("<h2 style='font-size: 16px; text-transform: uppercase; border-bottom: 1px solid #ccc; padding-bottom: 4px; margin-bottom: 12px;'>Education</h2>");
            for (JsonNode edu : edus) {
                String degree = getJsonString(edu, "degree", "title", "Degree");
                String school = getJsonString(edu, "institution", "school", "");
                String year = getJsonString(edu, "year", "dates", "");

                sb.append("<div style='display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px;'>");
                sb.append("<span><strong>").append(escape(degree)).append("</strong>").append(school.isEmpty() ? "" : ", " + escape(school)).append("</span>");
                sb.append("<span>").append(escape(year)).append("</span>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // Template 2: Professional (Corporate Dark Navy Header & Dividers)
    private String generateProfessionalHtml(String name, String email, String phone, String linkedin, String github, String portfolio,
                                            String summary, List<String> skills, List<JsonNode> edus, List<JsonNode> exps, List<JsonNode> projs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='tmpl-prof-container' style='font-family: Georgia, serif; color: #1e293b; padding: 0; max-width: 800px; margin: 0 auto; background: #fff; box-shadow: 0 4px 15px rgba(0,0,0,0.08); border-top: 6px solid #1e293b;'>");
        
        // Header
        sb.append("<div style='background: #f8fafc; padding: 25px 30px; border-bottom: 2px solid #e2e8f0;'>");
        sb.append("<h1 style='font-size: 28px; margin: 0; color: #0f172a; font-family: sans-serif; letter-spacing: 0.5px;'>").append(escape(name)).append("</h1>");
        sb.append("<div style='font-size: 13px; color: #475569; margin-top: 8px; font-family: sans-serif;'>");
        List<String> contacts = new ArrayList<>();
        if (!email.isEmpty()) contacts.add("✉ " + email);
        if (!phone.isEmpty()) contacts.add("📞 " + phone);
        if (!linkedin.isEmpty()) contacts.add("🔗 LinkedIn");
        if (!github.isEmpty()) contacts.add("💻 GitHub");
        sb.append(String.join(" &nbsp;|&nbsp; ", contacts));
        sb.append("</div></div>");

        sb.append("<div style='padding: 25px 30px;'>");

        if (!summary.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 14px; text-transform: uppercase; font-family: sans-serif; color: #1e293b; letter-spacing: 1px; border-bottom: 2px solid #1e293b; padding-bottom: 4px; margin-bottom: 8px;'>Executive Summary</h3>");
            sb.append("<p style='font-size: 13.5px; margin: 0; line-height: 1.6; color: #334155;'>").append(escape(summary)).append("</p>");
            sb.append("</div>");
        }

        if (!skills.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 14px; text-transform: uppercase; font-family: sans-serif; color: #1e293b; letter-spacing: 1px; border-bottom: 2px solid #1e293b; padding-bottom: 4px; margin-bottom: 8px;'>Core Competencies</h3>");
            sb.append("<div style='font-size: 13px; font-family: sans-serif; color: #334155;'>").append(String.join(" • ", skills)).append("</div>");
            sb.append("</div>");
        }

        if (!exps.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 14px; text-transform: uppercase; font-family: sans-serif; color: #1e293b; letter-spacing: 1px; border-bottom: 2px solid #1e293b; padding-bottom: 4px; margin-bottom: 12px;'>Professional Experience</h3>");
            for (JsonNode exp : exps) {
                String role = getJsonString(exp, "title", "role", "Position");
                String company = getJsonString(exp, "company", "organization", "");
                String duration = getJsonString(exp, "duration", "dates", "");
                String desc = getJsonString(exp, "description", "details", "");

                sb.append("<div style='margin-bottom: 14px;'>");
                sb.append("<div style='display: flex; justify-content: space-between; font-family: sans-serif; font-size: 14px; font-weight: bold;'>");
                sb.append("<span style='color: #0f172a;'>").append(escape(role)).append(company.isEmpty() ? "" : " <span style='font-weight: normal; color: #475569;'>at " + escape(company) + "</span>").append("</span>");
                sb.append("<span style='color: #64748b; font-size: 12px;'>").append(escape(duration)).append("</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) {
                    sb.append("<p style='font-size: 13px; margin: 4px 0 0 0; line-height: 1.5; color: #334155;'>").append(escape(desc)).append("</p>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        if (!projs.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 14px; text-transform: uppercase; font-family: sans-serif; color: #1e293b; letter-spacing: 1px; border-bottom: 2px solid #1e293b; padding-bottom: 4px; margin-bottom: 12px;'>Key Projects</h3>");
            for (JsonNode proj : projs) {
                String title = getJsonString(proj, "name", "title", "Project");
                String tech = getJsonString(proj, "technologies", "tech", "");
                String desc = getJsonString(proj, "description", "details", "");

                sb.append("<div style='margin-bottom: 10px;'>");
                sb.append("<div style='font-family: sans-serif; font-weight: bold; font-size: 13.5px; color: #0f172a;'>").append(escape(title));
                if (!tech.isEmpty()) sb.append("<span style='font-weight: normal; font-size: 12px; color: #64748b;'> | ").append(escape(tech)).append("</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) sb.append("<p style='font-size: 13px; margin: 2px 0 0 0; color: #334155;'>").append(escape(desc)).append("</p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        if (!edus.isEmpty()) {
            sb.append("<div style='margin-bottom: 15px;'>");
            sb.append("<h3 style='font-size: 14px; text-transform: uppercase; font-family: sans-serif; color: #1e293b; letter-spacing: 1px; border-bottom: 2px solid #1e293b; padding-bottom: 4px; margin-bottom: 10px;'>Education & Credentials</h3>");
            for (JsonNode edu : edus) {
                String degree = getJsonString(edu, "degree", "title", "Degree");
                String school = getJsonString(edu, "institution", "school", "");
                String year = getJsonString(edu, "year", "dates", "");

                sb.append("<div style='display: flex; justify-content: space-between; font-family: sans-serif; font-size: 13px; margin-bottom: 6px;'>");
                sb.append("<span style='color: #0f172a;'><strong>").append(escape(degree)).append("</strong>").append(school.isEmpty() ? "" : " - " + escape(school)).append("</span>");
                sb.append("<span style='color: #64748b;'>").append(escape(year)).append("</span>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("</div></div>");
        return sb.toString();
    }

    // Template 3: Modern (Indigo Accent Headers & Rounded Skill Tags)
    private String generateModernHtml(String name, String email, String phone, String linkedin, String github, String portfolio,
                                       String summary, List<String> skills, List<JsonNode> edus, List<JsonNode> exps, List<JsonNode> projs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='tmpl-modern-container' style='font-family: \"Outfit\", system-ui, sans-serif; color: #1e293b; padding: 30px; max-width: 800px; margin: 0 auto; background: #fff; border-radius: 12px; box-shadow: 0 5px 20px rgba(99, 102, 241, 0.08);'>");
        
        // Header
        sb.append("<div style='display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 3px solid #6366f1; padding-bottom: 18px; margin-bottom: 22px;'>");
        sb.append("<div>");
        sb.append("<h1 style='font-size: 28px; font-weight: 800; color: #312e81; margin: 0; letter-spacing: -0.5px;'>").append(escape(name)).append("</h1>");
        sb.append("<p style='color: #6366f1; font-weight: 600; font-size: 14px; margin: 4px 0 0 0;'>Professional Candidate</p>");
        sb.append("</div>");

        sb.append("<div style='font-size: 12.5px; color: #475569; text-align: right; line-height: 1.6;'>");
        if (!email.isEmpty()) sb.append("<div>").append(escape(email)).append("</div>");
        if (!phone.isEmpty()) sb.append("<div>").append(escape(phone)).append("</div>");
        if (!linkedin.isEmpty()) sb.append("<div>").append(escape(linkedin)).append("</div>");
        if (!github.isEmpty()) sb.append("<div>").append(escape(github)).append("</div>");
        sb.append("</div></div>");

        if (!summary.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; border-left: 4px solid #6366f1; padding-left: 10px; margin: 0 0 8px 0;'>About Me</h3>");
            sb.append("<p style='font-size: 13.5px; margin: 0; line-height: 1.6; color: #334155;'>").append(escape(summary)).append("</p>");
            sb.append("</div>");
        }

        if (!skills.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; border-left: 4px solid #6366f1; padding-left: 10px; margin: 0 0 10px 0;'>Skills & Expertise</h3>");
            sb.append("<div style='display: flex; flex-wrap: wrap; gap: 6px;'>");
            for (String s : skills) {
                sb.append("<span style='background: #e0e7ff; color: #3730a3; padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600;'>").append(escape(s)).append("</span>");
            }
            sb.append("</div></div>");
        }

        if (!exps.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; border-left: 4px solid #6366f1; padding-left: 10px; margin: 0 0 12px 0;'>Experience</h3>");
            for (JsonNode exp : exps) {
                String role = getJsonString(exp, "title", "role", "Position");
                String company = getJsonString(exp, "company", "organization", "");
                String duration = getJsonString(exp, "duration", "dates", "");
                String desc = getJsonString(exp, "description", "details", "");

                sb.append("<div style='margin-bottom: 14px; background: #f8fafc; padding: 12px 16px; border-radius: 8px; border: 1px solid #e2e8f0;'>");
                sb.append("<div style='display: flex; justify-content: space-between; font-size: 14px; font-weight: 700;'>");
                sb.append("<span style='color: #1e1b4b;'>").append(escape(role)).append(company.isEmpty() ? "" : " <span style='font-weight: 500; color: #6366f1;'>@ " + escape(company) + "</span>").append("</span>");
                sb.append("<span style='color: #64748b; font-size: 12px; font-weight: 500;'>").append(escape(duration)).append("</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) {
                    sb.append("<p style='font-size: 13px; margin: 6px 0 0 0; line-height: 1.5; color: #334155;'>").append(escape(desc)).append("</p>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        if (!projs.isEmpty()) {
            sb.append("<div style='margin-bottom: 22px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; border-left: 4px solid #6366f1; padding-left: 10px; margin: 0 0 12px 0;'>Featured Projects</h3>");
            for (JsonNode proj : projs) {
                String title = getJsonString(proj, "name", "title", "Project");
                String tech = getJsonString(proj, "technologies", "tech", "");
                String desc = getJsonString(proj, "description", "details", "");

                sb.append("<div style='margin-bottom: 10px;'>");
                sb.append("<div style='font-weight: 700; font-size: 14px; color: #1e1b4b;'>").append(escape(title));
                if (!tech.isEmpty()) sb.append(" <span style='font-weight: 400; font-size: 12px; color: #6366f1;'>[").append(escape(tech)).append("]</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) sb.append("<p style='font-size: 13px; margin: 2px 0 0 0; color: #475569;'>").append(escape(desc)).append("</p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        if (!edus.isEmpty()) {
            sb.append("<div style='margin-bottom: 10px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; border-left: 4px solid #6366f1; padding-left: 10px; margin: 0 0 10px 0;'>Education</h3>");
            for (JsonNode edu : edus) {
                String degree = getJsonString(edu, "degree", "title", "Degree");
                String school = getJsonString(edu, "institution", "school", "");
                String year = getJsonString(edu, "year", "dates", "");

                sb.append("<div style='display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px;'>");
                sb.append("<span style='color: #0f172a;'><strong>").append(escape(degree)).append("</strong>").append(school.isEmpty() ? "" : ", " + escape(school)).append("</span>");
                sb.append("<span style='color: #64748b;'>").append(escape(year)).append("</span>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // Template 4: Minimal (Monochrome & Generous Whitespace)
    private String generateMinimalHtml(String name, String email, String phone, String linkedin, String github, String portfolio,
                                        String summary, List<String> skills, List<JsonNode> edus, List<JsonNode> exps, List<JsonNode> projs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='tmpl-minimal-container' style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif; color: #334155; padding: 40px; max-width: 780px; margin: 0 auto; background: #fff; line-height: 1.6;'>");
        
        // Header
        sb.append("<div style='margin-bottom: 30px;'>");
        sb.append("<h1 style='font-size: 32px; font-weight: 300; color: #0f172a; margin: 0 0 6px 0; letter-spacing: -0.5px;'>").append(escape(name)).append("</h1>");
        sb.append("<div style='font-size: 12.5px; color: #64748b; font-weight: 400;'>");
        List<String> contacts = new ArrayList<>();
        if (!email.isEmpty()) contacts.add(email);
        if (!phone.isEmpty()) contacts.add(phone);
        if (!linkedin.isEmpty()) contacts.add(linkedin);
        if (!github.isEmpty()) contacts.add(github);
        sb.append(String.join(" &bull; ", contacts));
        sb.append("</div></div>");

        if (!summary.isEmpty()) {
            sb.append("<div style='margin-bottom: 28px;'>");
            sb.append("<p style='font-size: 14px; margin: 0; color: #475569; font-weight: 300; line-height: 1.7;'>").append(escape(summary)).append("</p>");
            sb.append("</div>");
        }

        if (!skills.isEmpty()) {
            sb.append("<div style='margin-bottom: 28px;'>");
            sb.append("<h4 style='font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 1.5px; color: #94a3b8; margin: 0 0 8px 0;'>Skills</h4>");
            sb.append("<p style='font-size: 13px; color: #334155; margin: 0;'>").append(String.join("  /  ", skills)).append("</p>");
            sb.append("</div>");
        }

        if (!exps.isEmpty()) {
            sb.append("<div style='margin-bottom: 28px;'>");
            sb.append("<h4 style='font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 1.5px; color: #94a3b8; margin: 0 0 14px 0;'>Experience</h4>");
            for (JsonNode exp : exps) {
                String role = getJsonString(exp, "title", "role", "Position");
                String company = getJsonString(exp, "company", "organization", "");
                String duration = getJsonString(exp, "duration", "dates", "");
                String desc = getJsonString(exp, "description", "details", "");

                sb.append("<div style='margin-bottom: 16px;'>");
                sb.append("<div style='display: flex; justify-content: space-between; font-size: 13.5px;'>");
                sb.append("<span style='font-weight: 600; color: #0f172a;'>").append(escape(role)).append(company.isEmpty() ? "" : ", " + escape(company)).append("</span>");
                sb.append("<span style='color: #94a3b8; font-size: 12px;'>").append(escape(duration)).append("</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) {
                    sb.append("<p style='font-size: 13px; margin: 4px 0 0 0; color: #475569; font-weight: 300;'>").append(escape(desc)).append("</p>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        if (!projs.isEmpty()) {
            sb.append("<div style='margin-bottom: 28px;'>");
            sb.append("<h4 style='font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 1.5px; color: #94a3b8; margin: 0 0 12px 0;'>Projects</h4>");
            for (JsonNode proj : projs) {
                String title = getJsonString(proj, "name", "title", "Project");
                String tech = getJsonString(proj, "technologies", "tech", "");
                String desc = getJsonString(proj, "description", "details", "");

                sb.append("<div style='margin-bottom: 12px;'>");
                sb.append("<div style='font-size: 13.5px; font-weight: 600; color: #0f172a;'>").append(escape(title));
                if (!tech.isEmpty()) sb.append(" <span style='font-weight: 300; font-size: 12px; color: #64748b;'>(").append(escape(tech)).append(")</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) sb.append("<p style='font-size: 13px; margin: 2px 0 0 0; color: #475569; font-weight: 300;'>").append(escape(desc)).append("</p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        if (!edus.isEmpty()) {
            sb.append("<div style='margin-bottom: 10px;'>");
            sb.append("<h4 style='font-size: 11px; text-transform: uppercase; font-weight: 700; letter-spacing: 1.5px; color: #94a3b8; margin: 0 0 10px 0;'>Education</h4>");
            for (JsonNode edu : edus) {
                String degree = getJsonString(edu, "degree", "title", "Degree");
                String school = getJsonString(edu, "institution", "school", "");
                String year = getJsonString(edu, "year", "dates", "");

                sb.append("<div style='display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px;'>");
                sb.append("<span style='color: #0f172a; font-weight: 500;'>").append(escape(degree)).append(school.isEmpty() ? "" : " &mdash; " + escape(school)).append("</span>");
                sb.append("<span style='color: #94a3b8;'>").append(escape(year)).append("</span>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // Template 5: Creative (Dual Column Layout with Dark Sidebar)
    private String generateCreativeHtml(String name, String email, String phone, String linkedin, String github, String portfolio,
                                         String summary, List<String> skills, List<JsonNode> edus, List<JsonNode> exps, List<JsonNode> projs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='tmpl-creative-container' style='font-family: \"Outfit\", sans-serif; display: flex; max-width: 820px; margin: 0 auto; background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1);'>");
        
        // Left Dark Sidebar
        sb.append("<div style='width: 32%; background: #1e1b4b; color: #f1f5f9; padding: 30px 20px; flex-shrink: 0;'>");
        sb.append("<h2 style='font-size: 22px; font-weight: 800; color: #a5b4fc; margin: 0 0 18px 0; line-height: 1.2;'>").append(escape(name)).append("</h2>");
        
        // Contact Info
        sb.append("<div style='margin-bottom: 25px; font-size: 12px; color: #c7d2fe;'>");
        sb.append("<h4 style='font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #818cf8; margin: 0 0 8px 0;'>Contact</h4>");
        if (!email.isEmpty()) sb.append("<div style='margin-bottom: 6px; word-break: break-all;'>✉ ").append(escape(email)).append("</div>");
        if (!phone.isEmpty()) sb.append("<div style='margin-bottom: 6px;'>📞 ").append(escape(phone)).append("</div>");
        if (!linkedin.isEmpty()) sb.append("<div style='margin-bottom: 6px;'>🔗 ").append(escape(linkedin)).append("</div>");
        if (!github.isEmpty()) sb.append("<div style='margin-bottom: 6px;'>💻 ").append(escape(github)).append("</div>");
        sb.append("</div>");

        // Skills Tags in Sidebar
        if (!skills.isEmpty()) {
            sb.append("<div style='margin-bottom: 25px;'>");
            sb.append("<h4 style='font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #818cf8; margin: 0 0 10px 0;'>Expertise</h4>");
            sb.append("<div style='display: flex; flex-wrap: wrap; gap: 4px;'>");
            for (String s : skills) {
                sb.append("<span style='background: #312e81; color: #e0e7ff; padding: 3px 8px; border-radius: 4px; font-size: 11px;'>").append(escape(s)).append("</span>");
            }
            sb.append("</div></div>");
        }

        // Education in Sidebar
        if (!edus.isEmpty()) {
            sb.append("<div>");
            sb.append("<h4 style='font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: #818cf8; margin: 0 0 10px 0;'>Education</h4>");
            for (JsonNode edu : edus) {
                String degree = getJsonString(edu, "degree", "title", "Degree");
                String school = getJsonString(edu, "institution", "school", "");
                String year = getJsonString(edu, "year", "dates", "");

                sb.append("<div style='margin-bottom: 10px; font-size: 12px;'>");
                sb.append("<div style='font-weight: bold; color: #fff;'>").append(escape(degree)).append("</div>");
                if (!school.isEmpty()) sb.append("<div style='color: #a5b4fc;'>").append(escape(school)).append("</div>");
                if (!year.isEmpty()) sb.append("<div style='color: #818cf8; font-size: 11px;'>").append(escape(year)).append("</div>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("</div>"); // End Sidebar

        // Right Main Content
        sb.append("<div style='width: 68%; padding: 30px 25px;'>");

        if (!summary.isEmpty()) {
            sb.append("<div style='margin-bottom: 24px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; margin: 0 0 8px 0; border-bottom: 2px solid #e0e7ff; padding-bottom: 4px;'>Profile Summary</h3>");
            sb.append("<p style='font-size: 13px; margin: 0; line-height: 1.6; color: #334155;'>").append(escape(summary)).append("</p>");
            sb.append("</div>");
        }

        if (!exps.isEmpty()) {
            sb.append("<div style='margin-bottom: 24px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; margin: 0 0 12px 0; border-bottom: 2px solid #e0e7ff; padding-bottom: 4px;'>Work History</h3>");
            for (JsonNode exp : exps) {
                String role = getJsonString(exp, "title", "role", "Position");
                String company = getJsonString(exp, "company", "organization", "");
                String duration = getJsonString(exp, "duration", "dates", "");
                String desc = getJsonString(exp, "description", "details", "");

                sb.append("<div style='margin-bottom: 14px;'>");
                sb.append("<div style='display: flex; justify-content: space-between; font-size: 13.5px;'>");
                sb.append("<span style='font-weight: 700; color: #1e1b4b;'>").append(escape(role)).append(company.isEmpty() ? "" : " <span style='font-weight: 500; color: #6366f1;'>@ " + escape(company) + "</span>").append("</span>");
                sb.append("<span style='color: #64748b; font-size: 11.5px;'>").append(escape(duration)).append("</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) {
                    sb.append("<p style='font-size: 12.5px; margin: 4px 0 0 0; line-height: 1.5; color: #475569;'>").append(escape(desc)).append("</p>");
                }
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        if (!projs.isEmpty()) {
            sb.append("<div style='margin-bottom: 15px;'>");
            sb.append("<h3 style='font-size: 15px; font-weight: 700; color: #4338ca; margin: 0 0 12px 0; border-bottom: 2px solid #e0e7ff; padding-bottom: 4px;'>Projects & Achievements</h3>");
            for (JsonNode proj : projs) {
                String title = getJsonString(proj, "name", "title", "Project");
                String tech = getJsonString(proj, "technologies", "tech", "");
                String desc = getJsonString(proj, "description", "details", "");

                sb.append("<div style='margin-bottom: 10px;'>");
                sb.append("<div style='font-weight: 700; font-size: 13px; color: #1e1b4b;'>").append(escape(title));
                if (!tech.isEmpty()) sb.append(" <span style='font-weight: 400; font-size: 11px; color: #6366f1;'>[").append(escape(tech)).append("]</span>");
                sb.append("</div>");
                if (!desc.isEmpty()) sb.append("<p style='font-size: 12.5px; margin: 2px 0 0 0; color: #475569;'>").append(escape(desc)).append("</p>");
                sb.append("</div>");
            }
            sb.append("</div>");
        }

        sb.append("</div></div>");
        return sb.toString();
    }

    private String getJsonString(JsonNode node, String k1, String k2, String def) {
        if (node == null) return def;
        if (node.has(k1) && !node.get(k1).isNull() && !node.get(k1).asText().trim().isEmpty()) {
            return node.get(k1).asText().trim();
        }
        if (node.has(k2) && !node.get(k2).isNull() && !node.get(k2).asText().trim().isEmpty()) {
            return node.get(k2).asText().trim();
        }
        return def;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
