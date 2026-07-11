// ----------------------------------------------------
// STATE MANAGEMENT
// ----------------------------------------------------
const state = {
    currentView: 'dashboard',
    profile: {
        fullName: 'Guest User',
        email: 'guest@career.com',
        phone: '',
        linkedinUrl: '',
        githubUrl: '',
        portfolioUrl: '',
        skills: '[]',
        education: '[]',
        experience: '[]',
        projects: '[]'
    },
    resumes: []
};

// ----------------------------------------------------
// INITIALIZATION
// ----------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    initRouting();
    initUpload();
    initProfileActions();
    loadProfile();
    loadResumes();
});

// ----------------------------------------------------
// VIEW ROUTING
// ----------------------------------------------------
function initRouting() {
    const navItems = document.querySelectorAll('.nav-item');
    const views = document.querySelectorAll('.content-view');

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const targetView = item.getAttribute('data-view');
            
            // Update active menu link
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');
            
            // Switch view panel
            views.forEach(v => v.classList.remove('active'));
            const activePanel = document.getElementById(`view-${targetView}`);
            if (activePanel) {
                activePanel.classList.add('active');
                state.currentView = targetView;
            }

            // Custom tab actions
            if (targetView === 'resumes') {
                loadResumes();
            } else if (targetView === 'profile') {
                loadProfile();
            }
        });
    });

    // Handle "View All" link on dashboard
    document.getElementById('link-view-all-resumes').addEventListener('click', (e) => {
        e.preventDefault();
        const resumesMenu = document.querySelector('.nav-item[data-view="resumes"]');
        if (resumesMenu) resumesMenu.click();
    });
}

// ----------------------------------------------------
// DATA FETCHING: PROFILE & HISTORY
// ----------------------------------------------------
async function loadProfile() {
    try {
        const response = await fetch('/api/profile');
        if (!response.ok) throw new Error('Failed to load profile');
        const data = await response.json();
        
        state.profile = data;
        updateProfileUI();
        updateStats();
    } catch (err) {
        console.error(err);
        showNotification('Error loading career profile details.', 'danger');
    }
}

async function loadResumes() {
    try {
        const response = await fetch('/api/resumes/history');
        if (!response.ok) throw new Error('Failed to load history');
        const data = await response.json();
        
        state.resumes = data;
        updateResumesUI();
        updateStats();
    } catch (err) {
        console.error(err);
        showNotification('Error loading resume history.', 'danger');
    }
}

// ----------------------------------------------------
// PROFILE UI & BINDING
// ----------------------------------------------------
function updateProfileUI() {
    const prof = state.profile;

    // Set Welcome Display
    const firstName = prof.fullName ? prof.fullName.split(' ')[0] : 'Guest';
    document.getElementById('welcome-name').textContent = firstName;
    document.getElementById('user-display-name').textContent = prof.fullName || 'Guest User';

    // Form inputs
    document.getElementById('prof-name').value = prof.fullName || '';
    document.getElementById('prof-email').value = prof.email || '';
    document.getElementById('prof-phone').value = prof.phone || '';
    document.getElementById('prof-linkedin').value = prof.linkedinUrl || '';
    document.getElementById('prof-github').value = prof.githubUrl || '';
    document.getElementById('prof-portfolio').value = prof.portfolioUrl || '';

    // Render list sections
    renderSkillsTags();
    renderEducationCards();
    renderExperienceCards();
    renderProjectsCards();
}

function parseJsonField(field) {
    if (!field) return [];
    try {
        return typeof field === 'string' ? JSON.parse(field) : field;
    } catch (e) {
        return [];
    }
}

// Render dynamic Lists in profile page
function renderSkillsTags() {
    const container = document.getElementById('profile-skills-container');
    container.innerHTML = '';
    const skills = parseJsonField(state.profile.skills);
    
    if (skills.length === 0) {
        container.innerHTML = '<p class="text-muted text-center py-2 w-100">No skills listed. Upload a resume or add some manually!</p>';
        return;
    }

    skills.forEach((skill, idx) => {
        const tag = document.createElement('span');
        tag.className = 'skill-tag';
        tag.innerHTML = `
            ${escapeHtml(skill)}
            <button class="btn-remove-skill" onclick="removeSkill(${idx})">
                <i class="fa-solid fa-xmark"></i>
            </button>
        `;
        container.appendChild(tag);
    });
}

function renderEducationCards() {
    const container = document.getElementById('profile-education-container');
    container.innerHTML = '';
    const edus = parseJsonField(state.profile.education);

    if (edus.length === 0) {
        container.innerHTML = '<div class="empty-state py-3"><i class="fa-solid fa-graduation-cap"></i><p>No education details listed.</p></div>';
        return;
    }

    edus.forEach((edu, idx) => {
        const card = document.createElement('div');
        card.className = 'profile-card-item';
        card.innerHTML = `
            <button class="item-delete-btn" onclick="removeEdu(${idx})"><i class="fa-solid fa-trash-can"></i></button>
            <h4>${escapeHtml(edu.degree || 'Degree Not Specified')}</h4>
            <h5>${escapeHtml(edu.institution || 'Institution Not Specified')}</h5>
            <div class="item-dates">
                <i class="fa-solid fa-calendar-days"></i> 
                <span>${escapeHtml(edu.fieldOfStudy || '')} ${edu.endDate ? '| Graduated ' + escapeHtml(edu.endDate) : ''}</span>
            </div>
            ${edu.gpa ? `<p>GPA: ${escapeHtml(edu.gpa)}</p>` : ''}
        `;
        container.appendChild(card);
    });
}

function renderExperienceCards() {
    const container = document.getElementById('profile-experience-container');
    container.innerHTML = '';
    const exps = parseJsonField(state.profile.experience);

    if (exps.length === 0) {
        container.innerHTML = '<div class="empty-state py-3"><i class="fa-solid fa-briefcase"></i><p>No work experience details listed.</p></div>';
        return;
    }

    exps.forEach((exp, idx) => {
        const card = document.createElement('div');
        card.className = 'profile-card-item';
        
        let bulletsHtml = '';
        const responsibilities = parseJsonField(exp.responsibilities);
        if (responsibilities && responsibilities.length > 0) {
            bulletsHtml = '<ul>';
            responsibilities.forEach(bullet => {
                bulletsHtml += `<li>${escapeHtml(bullet)}</li>`;
            });
            bulletsHtml += '</ul>';
        }

        card.innerHTML = `
            <button class="item-delete-btn" onclick="removeExp(${idx})"><i class="fa-solid fa-trash-can"></i></button>
            <h4>${escapeHtml(exp.jobTitle || 'Job Title')}</h4>
            <h5>${escapeHtml(exp.company || 'Company')}</h5>
            <div class="item-dates">
                <i class="fa-solid fa-calendar-days"></i> 
                <span>${escapeHtml(exp.startDate || '')} - ${escapeHtml(exp.endDate || 'Present')}</span>
            </div>
            ${bulletsHtml}
        `;
        container.appendChild(card);
    });
}

function renderProjectsCards() {
    const container = document.getElementById('profile-projects-container');
    container.innerHTML = '';
    const projs = parseJsonField(state.profile.projects);

    if (projs.length === 0) {
        container.innerHTML = '<div class="empty-state py-3"><i class="fa-solid fa-diagram-project"></i><p>No project details listed.</p></div>';
        return;
    }

    projs.forEach((proj, idx) => {
        const card = document.createElement('div');
        card.className = 'profile-card-item';

        let techTags = '';
        const technologies = parseJsonField(proj.technologies);
        if (technologies && technologies.length > 0) {
            techTags = '<div class="report-skills-tags mt-2">';
            technologies.forEach(tech => {
                techTags += `<span class="report-skill">${escapeHtml(tech)}</span>`;
            });
            techTags += '</div>';
        }

        card.innerHTML = `
            <button class="item-delete-btn" onclick="removeProject(${idx})"><i class="fa-solid fa-trash-can"></i></button>
            <h4>${escapeHtml(proj.title || 'Project Title')}</h4>
            <p>${escapeHtml(proj.description || 'No description provided.')}</p>
            ${techTags}
        `;
        container.appendChild(card);
    });
}

// Stats & Completion Calculator
function updateStats() {
    const total = state.resumes.length;
    const parsed = state.resumes.filter(r => r.parseStatus === 'SUCCESS').length;
    
    document.getElementById('stat-total-resumes').textContent = total;
    document.getElementById('stat-parsed-ok').textContent = parsed;

    // Calculate profile completeness
    const prof = state.profile;
    let fields = 0;
    let filled = 0;

    const basicFields = [prof.fullName, prof.email, prof.phone, prof.linkedinUrl, prof.githubUrl, prof.portfolioUrl];
    fields += basicFields.length;
    basicFields.forEach(f => { if (f && f.trim() !== '') filled++; });

    const skills = parseJsonField(prof.skills);
    fields += 1; if (skills.length > 0) filled++;

    const edu = parseJsonField(prof.education);
    fields += 1; if (edu.length > 0) filled++;

    const exp = parseJsonField(prof.experience);
    fields += 1; if (exp.length > 0) filled++;

    const proj = parseJsonField(prof.projects);
    fields += 1; if (proj.length > 0) filled++;

    const percentage = Math.round((filled / fields) * 100);
    document.getElementById('stat-profile-comp').textContent = `${percentage}%`;

    // Dynamic ATS rating based on completeness and parsed values
    let atsRating = '--';
    if (parsed > 0) {
        // Base score starts at 55
        let base = 55;
        if (skills.length > 5) base += 10;
        else if (skills.length > 0) base += 5;
        
        if (exp.length > 0) base += 15;
        if (edu.length > 0) base += 10;
        if (proj.length > 0) base += 10;

        // Cap at 95 for mock offline checker
        atsRating = Math.min(base, 95);
    }
    document.getElementById('stat-ats-score').textContent = atsRating;
}

// ----------------------------------------------------
// RESUME HISTORY UI RENDERING
// ----------------------------------------------------
function updateResumesUI() {
    const recentContainer = document.getElementById('recent-resumes-list');
    const tableBody = document.getElementById('resumes-table-body');

    // 1. Dashboard View (Recent 3 Resumes)
    recentContainer.innerHTML = '';
    const recents = state.resumes.slice(0, 3);
    
    if (recents.length === 0) {
        recentContainer.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-folder-open"></i>
                <p>No resumes uploaded yet.</p>
            </div>`;
    } else {
        recents.forEach(resume => {
            const item = document.createElement('div');
            item.className = 'recent-item';
            
            const fileIcon = getFileIconClass(resume.fileType);
            const statusClass = resume.parseStatus.toLowerCase();
            const uploadTimeStr = formatDate(resume.uploadDate);

            item.innerHTML = `
                <div class="item-left">
                    <i class="${fileIcon} item-icon"></i>
                    <div class="item-meta">
                        <h5>${escapeHtml(resume.fileName)}</h5>
                        <p>${uploadTimeStr} | ${(resume.fileSize / 1024).toFixed(1)} KB</p>
                    </div>
                </div>
                <div class="item-right">
                    <span class="status-badge ${statusClass}">${resume.parseStatus}</span>
                    <button class="btn btn-sm btn-outline" onclick="viewResumeDetails(${resume.id})">Report</button>
                </div>
            `;
            recentContainer.appendChild(item);
        });
    }

    // 2. Full History View Table
    tableBody.innerHTML = '';
    if (state.resumes.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-5">
                    <i class="fa-solid fa-folder-open" style="font-size: 2rem; opacity: 0.3; display: block; margin-bottom: 0.5rem;"></i>
                    No resumes found. Go to the dashboard to upload your first resume!
                </td>
            </tr>`;
    } else {
        state.resumes.forEach(resume => {
            const tr = document.createElement('tr');
            const fileIcon = getFileIconClass(resume.fileType);
            const statusClass = resume.parseStatus.toLowerCase();

            tr.innerHTML = `
                <td>
                    <span style="display: inline-flex; align-items: center; gap: 0.6rem;">
                        <i class="${fileIcon}" style="color: var(--primary); font-size: 1.1rem;"></i>
                        <strong>${escapeHtml(resume.fileName)}</strong>
                    </span>
                </td>
                <td>${formatDate(resume.uploadDate)}</td>
                <td>${(resume.fileSize / 1024).toFixed(1)} KB</td>
                <td><span class="status-badge ${statusClass}">${resume.parseStatus}</span></td>
                <td class="actions">
                    <button class="btn btn-sm btn-outline" onclick="viewResumeDetails(${resume.id})">
                        <i class="fa-solid fa-magnifying-glass-chart"></i> View Report
                    </button>
                    <button class="btn-danger-icon" onclick="deleteResume(${resume.id})" title="Delete File">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </td>
            `;
            tableBody.appendChild(tr);
        });
    }
}

// ----------------------------------------------------
// RESUME UPLOAD FLOW
// ----------------------------------------------------
function initUpload() {
    const dropZone = document.getElementById('drop-zone');
    const fileInput = document.getElementById('file-input');
    const btnBrowse = document.getElementById('btn-browse');

    btnBrowse.addEventListener('click', () => fileInput.click());
    
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleUpload(e.target.files[0]);
        }
    });

    // Drag-and-drop Events
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('dragover');
    });

    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('dragover');
    });

    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            handleUpload(e.dataTransfer.files[0]);
        }
    });
}

function handleUpload(file) {
    // 1. Validation
    const allowedTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/msword'];
    if (!allowedTypes.includes(file.type)) {
        showNotification('Invalid file type! Please upload a PDF or DOCX file.', 'danger');
        return;
    }

    if (file.size > 10 * 1024 * 1024) {
        showNotification('File is too large! Maximum limit is 10MB.', 'danger');
        return;
    }

    // 2. Setup Progress UI
    const progressContainer = document.getElementById('upload-progress-container');
    const filenameEl = document.getElementById('upload-filename');
    const filesizeEl = document.getElementById('upload-filesize');
    const progressFill = document.getElementById('progress-bar-fill');
    const progressText = document.getElementById('progress-text');
    const progressPercent = document.getElementById('progress-percent');

    filenameEl.textContent = file.name;
    filesizeEl.textContent = `${(file.size / 1024).toFixed(1)} KB`;
    progressFill.style.width = '0%';
    progressText.textContent = 'Uploading file...';
    progressPercent.textContent = '0%';
    progressContainer.style.display = 'block';

    const fileIcon = document.getElementById('upload-file-icon');
    fileIcon.className = getFileIconClass(file.type);

    // 3. Perform AJAX Upload
    const formData = new FormData();
    formData.append('file', file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/resumes/upload', true);

    // Monitor upload progress
    xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
            const percentComplete = Math.round((e.loaded / e.total) * 100);
            // Limit fake upload progress to 90% until server parses it
            const displayPercent = Math.min(percentComplete, 90);
            progressFill.style.width = `${displayPercent}%`;
            progressPercent.textContent = `${displayPercent}%`;
            if (displayPercent === 90) {
                progressText.textContent = 'Extracting and parsing text...';
            }
        }
    };

    xhr.onload = () => {
        if (xhr.status === 200) {
            progressFill.style.width = '100%';
            progressPercent.textContent = '100%';
            progressText.textContent = 'Parsing complete!';
            
            showNotification('Resume uploaded and parsed successfully!', 'success');
            
            // Reload tables and profile dashboard
            setTimeout(() => {
                progressContainer.style.display = 'none';
                loadProfile();
                loadResumes();
            }, 1000);
        } else {
            let errorMsg = 'Upload failed.';
            try {
                errorMsg = xhr.responseText || 'Upload failed.';
            } catch(e){}
            showNotification(errorMsg, 'danger');
            progressText.textContent = 'Parsing failed.';
            progressFill.style.backgroundColor = 'var(--danger)';
        }
    };

    xhr.onerror = () => {
        showNotification('Connection error during upload.', 'danger');
        progressText.textContent = 'Error connecting.';
    };

    xhr.send(formData);
}

// ----------------------------------------------------
// PROFILE ACTIONS: UPDATE & EDIT
// ----------------------------------------------------
function initProfileActions() {
    const btnSave = document.getElementById('btn-save-profile');
    btnSave.addEventListener('click', saveProfileData);

    // Skills Add Modal toggles
    const btnAddSkill = document.getElementById('btn-add-skill');
    const btnConfirmAddSkill = document.getElementById('btn-confirm-add-skill');
    const newSkillInput = document.getElementById('new-skill-input');
    const skillInputGroup = document.getElementById('skill-input-container');

    btnAddSkill.addEventListener('click', () => {
        skillInputGroup.style.display = skillInputGroup.style.display === 'none' ? 'flex' : 'none';
        newSkillInput.focus();
    });

    btnConfirmAddSkill.addEventListener('click', () => {
        const val = newSkillInput.value.trim();
        if (val) {
            const skills = parseJsonField(state.profile.skills);
            skills.push(val);
            state.profile.skills = JSON.stringify(skills);
            newSkillInput.value = '';
            skillInputGroup.style.display = 'none';
            renderSkillsTags();
            updateStats();
        }
    });

    newSkillInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') btnConfirmAddSkill.click();
    });

    // Add handlers for Education, Experience, Projects dialog templates
    document.getElementById('btn-add-edu').addEventListener('click', addEduPlaceholder);
    document.getElementById('btn-add-exp').addEventListener('click', addExpPlaceholder);
    document.getElementById('btn-add-project').addEventListener('click', addProjPlaceholder);
}

async function saveProfileData() {
    // Gather values from UI
    const prof = state.profile;
    prof.fullName = document.getElementById('prof-name').value;
    prof.email = document.getElementById('prof-email').value;
    prof.phone = document.getElementById('prof-phone').value;
    prof.linkedinUrl = document.getElementById('prof-linkedin').value;
    prof.githubUrl = document.getElementById('prof-github').value;
    prof.portfolioUrl = document.getElementById('prof-portfolio').value;

    try {
        const response = await fetch('/api/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(prof)
        });

        if (!response.ok) throw new Error('Update failed');
        const data = await response.json();
        
        state.profile = data;
        updateProfileUI();
        showNotification('Profile updated successfully!', 'success');
    } catch(err) {
        console.error(err);
        showNotification('Failed to save profile changes.', 'danger');
    }
}

// Skill list removals (Global bindings)
window.removeSkill = function(index) {
    const skills = parseJsonField(state.profile.skills);
    skills.splice(index, 1);
    state.profile.skills = JSON.stringify(skills);
    renderSkillsTags();
    updateStats();
};

// Item Add Placeholders for clean simulation
function addEduPlaceholder() {
    const edus = parseJsonField(state.profile.education);
    edus.push({
        degree: 'Bachelor of Science in Computer Science',
        institution: 'State University',
        fieldOfStudy: 'Computer Science',
        endDate: '2025',
        gpa: '3.8'
    });
    state.profile.education = JSON.stringify(edus);
    renderEducationCards();
    updateStats();
    showNotification('Added template education item. Please click "Save Profile" to submit.', 'info');
}

window.removeEdu = function(index) {
    const edus = parseJsonField(state.profile.education);
    edus.splice(index, 1);
    state.profile.education = JSON.stringify(edus);
    renderEducationCards();
    updateStats();
};

function addExpPlaceholder() {
    const exps = parseJsonField(state.profile.experience);
    exps.push({
        jobTitle: 'Junior Software Engineer',
        company: 'InnovateTech Corp',
        startDate: '2023',
        endDate: 'Present',
        responsibilities: [
            'Collaborated with a team of developers to deliver clean, scalable features.',
            'Wrote comprehensive unit tests and automated build integration pipelines.'
        ]
    });
    state.profile.experience = JSON.stringify(exps);
    renderExperienceCards();
    updateStats();
    showNotification('Added template experience item. Please click "Save Profile" to submit.', 'info');
}

window.removeExp = function(index) {
    const exps = parseJsonField(state.profile.experience);
    exps.splice(index, 1);
    state.profile.experience = JSON.stringify(exps);
    renderExperienceCards();
    updateStats();
};

function addProjPlaceholder() {
    const projs = parseJsonField(state.profile.projects);
    projs.push({
        title: 'E-Commerce Microservice API',
        description: 'Designed a high-throughput API gateway resolving inventory management transactions.',
        technologies: ['Java', 'Spring Cloud', 'PostgreSQL', 'Docker']
    });
    state.profile.projects = JSON.stringify(projs);
    renderProjectsCards();
    updateStats();
    showNotification('Added template project item. Please click "Save Profile" to submit.', 'info');
}

window.removeProject = function(index) {
    const projs = parseJsonField(state.profile.projects);
    projs.splice(index, 1);
    state.profile.projects = JSON.stringify(projs);
    renderProjectsCards();
    updateStats();
};

// ----------------------------------------------------
// DELETE RESUME ACTION
// ----------------------------------------------------
window.deleteResume = async function(id) {
    if (!confirm('Are you sure you want to delete this resume? This cannot be undone.')) {
        return;
    }

    try {
        const response = await fetch(`/api/resumes/${id}`, { method: 'DELETE' });
        if (!response.ok) throw new Error('Delete failed');
        
        showNotification('Resume deleted successfully.', 'success');
        loadResumes();
    } catch(err) {
        console.error(err);
        showNotification('Failed to delete resume.', 'danger');
    }
};

// ----------------------------------------------------
// MODAL DETAILS DRAWER VIEW
// ----------------------------------------------------
window.viewResumeDetails = async function(id) {
    const modal = document.getElementById('parsed-details-modal');
    const titleEl = document.getElementById('modal-resume-title');
    const metaEl = document.getElementById('modal-resume-meta');
    const jsonEl = document.getElementById('raw-json-content');

    // 1. Show modal overlay
    modal.classList.add('open');

    // Reset tabs to default (Structured View)
    const tabs = modal.querySelectorAll('.modal-tab');
    const tabContents = modal.querySelectorAll('.tab-content');
    tabs.forEach(t => t.classList.remove('active'));
    tabContents.forEach(c => c.classList.remove('active'));
    
    const defaultTab = modal.querySelector('.modal-tab[data-tab="tab-structured"]');
    if (defaultTab) defaultTab.classList.add('active');
    const defaultContent = document.getElementById('tab-structured');
    if (defaultContent) defaultContent.classList.add('active');

    // Reset Job Description match tab fields
    const jdTextarea = document.getElementById('jd-textarea');
    const matchResultsSection = document.getElementById('match-results-section');
    const jdFileInput = document.getElementById('jd-file-input');
    const jdFileStatus = document.getElementById('jd-file-status');
    if (jdTextarea) jdTextarea.value = '';
    if (matchResultsSection) matchResultsSection.style.display = 'none';
    if (jdFileStatus) jdFileStatus.textContent = 'No file chosen';
    if (jdFileInput) jdFileInput.value = '';

    // Handle JD file selection
    if (jdFileInput) {
        jdFileInput.onchange = (e) => {
            if (e.target.files.length > 0) {
                const file = e.target.files[0];
                jdFileStatus.textContent = file.name;
                const reader = new FileReader();
                reader.onload = (event) => {
                    jdTextarea.value = event.target.result;
                };
                reader.readAsText(file);
            }
        };
    }

    // Bind JD Match execution button
    const btnRunMatch = document.getElementById('btn-run-match');
    if (btnRunMatch) {
        btnRunMatch.onclick = async () => {
            const jdText = jdTextarea.value.trim();
            if (!jdText) {
                showNotification('Please paste or load a Job Description first.', 'danger');
                return;
            }
            btnRunMatch.disabled = true;
            btnRunMatch.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Calculating...';
            try {
                const res = await fetch(`/api/resumes/${id}/match`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ jobDescription: jdText })
                });
                if (!res.ok) throw new Error('Matching failed');
                const matchData = await res.json();
                renderMatchTab(matchData);
            } catch(err) {
                console.error(err);
                showNotification('Failed to compute job match.', 'danger');
            } finally {
                btnRunMatch.disabled = false;
                btnRunMatch.innerHTML = '<i class="fa-solid fa-calculator"></i> Calculate Match';
            }
        };
    }

    // 2. Fetch details from server
    try {
        const response = await fetch(`/api/resumes/${id}`);
        if (!response.ok) throw new Error('Failed to load resume details.');
        const resume = await response.json();

        // 3. Update title metadata
        titleEl.textContent = `Report: ${resume.fileName}`;
        metaEl.textContent = `Uploaded: ${formatDate(resume.uploadDate)} | Status: ${resume.parseStatus}`;

        // 4. Render Raw JSON Code View
        const parsed = JSON.parse(resume.parsedContent || '{}');
        jsonEl.textContent = JSON.stringify(parsed, null, 2);

        // 5. Render Structured View Fields
        document.getElementById('rep-name').textContent = parsed.name || 'Not extracted';
        document.getElementById('rep-email').textContent = parsed.email || 'Not extracted';
        document.getElementById('rep-phone').textContent = parsed.phone || 'Not extracted';
        document.getElementById('rep-linkedin').textContent = parsed.linkedin || 'Not extracted';
        document.getElementById('rep-github').textContent = parsed.github || 'Not extracted';
        document.getElementById('rep-portfolio').textContent = parsed.portfolio || 'Not extracted';

        // Render Skills tags
        const repSkillsContainer = document.getElementById('rep-skills');
        repSkillsContainer.innerHTML = '';
        if (parsed.skills && parsed.skills.length > 0) {
            parsed.skills.forEach(skill => {
                const span = document.createElement('span');
                span.className = 'report-skill';
                span.textContent = skill;
                repSkillsContainer.appendChild(span);
            });
        } else {
            repSkillsContainer.innerHTML = '<span class="text-muted">No skills parsed</span>';
        }

        // Render Education list
        const repEduContainer = document.getElementById('rep-education');
        repEduContainer.innerHTML = '';
        if (parsed.education && parsed.education.length > 0) {
            parsed.education.forEach(edu => {
                const div = document.createElement('div');
                div.className = 'report-item';
                div.innerHTML = `
                    <h5>${escapeHtml(edu.degree || 'Degree')}</h5>
                    <h6>${escapeHtml(edu.institution || 'Institution')}</h6>
                    <div class="dates">${escapeHtml(edu.fieldOfStudy || '')} ${edu.endDate ? '| ' + escapeHtml(edu.endDate) : ''}</div>
                    ${edu.gpa ? `<p>GPA: ${escapeHtml(edu.gpa)}</p>` : ''}
                `;
                repEduContainer.appendChild(div);
            });
        } else {
            repEduContainer.innerHTML = '<div class="text-muted">No education history parsed</div>';
        }

        // Render Experience list
        const repExpContainer = document.getElementById('rep-experience');
        repExpContainer.innerHTML = '';
        if (parsed.experience && parsed.experience.length > 0) {
            parsed.experience.forEach(exp => {
                const div = document.createElement('div');
                div.className = 'report-item';
                
                let bulletsHtml = '';
                if (exp.responsibilities && exp.responsibilities.length > 0) {
                    bulletsHtml = '<ul style="list-style: disc; padding-left: 1.2rem; margin-top: 0.4rem; font-size: 0.75rem; color: var(--text-muted);">';
                    exp.responsibilities.forEach(bullet => {
                        bulletsHtml += `<li>${escapeHtml(bullet)}</li>`;
                    });
                    bulletsHtml += '</ul>';
                }

                div.innerHTML = `
                    <h5>${escapeHtml(exp.jobTitle || 'Job Title')}</h5>
                    <h6>${escapeHtml(exp.company || 'Company')}</h6>
                    <div class="dates">${escapeHtml(exp.startDate || '')} - ${escapeHtml(exp.endDate || '')}</div>
                    ${bulletsHtml}
                `;
                repExpContainer.appendChild(div);
            });
        } else {
            repExpContainer.innerHTML = '<div class="text-muted">No experience history parsed</div>';
        }

        // Render Projects list
        const repProjContainer = document.getElementById('rep-projects');
        repProjContainer.innerHTML = '';
        if (parsed.projects && parsed.projects.length > 0) {
            parsed.projects.forEach(proj => {
                const div = document.createElement('div');
                div.className = 'report-item';

                let techTags = '';
                if (proj.technologies && proj.technologies.length > 0) {
                    techTags = '<div class="report-skills-tags mt-2">';
                    proj.technologies.forEach(tech => {
                        techTags += `<span class="report-skill">${escapeHtml(tech)}</span>`;
                    });
                    techTags += '</div>';
                }

                div.innerHTML = `
                    <h5>${escapeHtml(proj.title || 'Project Title')}</h5>
                    <p>${escapeHtml(proj.description || '')}</p>
                    ${techTags}
                `;
                repProjContainer.appendChild(div);
            });
        } else {
            repProjContainer.innerHTML = '<div class="text-muted">No projects parsed</div>';
        }

        // Fetch ATS Report Metrics
        fetch(`/api/resumes/${id}/ats`)
            .then(res => res.json())
            .then(atsData => renderAtsTab(atsData))
            .catch(err => console.error("Error loading ATS data:", err));

        // Fetch AI Analysis Report Findings
        fetch(`/api/resumes/${id}/ai-analysis`)
            .then(res => res.json())
            .then(aiData => renderAiTab(aiData))
            .catch(err => console.error("Error loading AI analysis:", err));

    } catch (err) {
        console.error(err);
        jsonEl.textContent = 'Error loading file report details.';
    }

    // Bind tab switching
    tabs.forEach(tab => {
        tab.onclick = () => {
            tabs.forEach(t => t.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            
            tab.classList.add('active');
            const targetContent = document.getElementById(tab.getAttribute('data-tab'));
            if (targetContent) targetContent.classList.add('active');
        };
    });

    // Close buttons
    const closeBtn = document.getElementById('btn-close-modal');
    const closeBtnFooter = document.getElementById('btn-close-modal-footer');
    
    const closeModal = () => modal.classList.remove('open');
    closeBtn.onclick = closeModal;
    closeBtnFooter.onclick = closeModal;
}

// ----------------------------------------------------
// TAB RENDERING ENGINE HELPERS
// ----------------------------------------------------
function renderAtsTab(data) {
    const scoreVal = data.atsScore || 0;
    const scoreValEl = document.getElementById('ats-score-value');
    if (scoreValEl) scoreValEl.textContent = `${scoreVal}%`;
    
    const ring = document.getElementById('ats-score-ring');
    if (ring) {
        ring.style.background = `conic-gradient(var(--primary) ${scoreVal * 3.6}deg, rgba(255, 255, 255, 0.05) 0deg)`;
    }

    const badge = document.getElementById('ats-compatibility-badge');
    if (badge) {
        badge.textContent = data.compatibility || 'UNKNOWN';
        badge.className = 'badge'; // Reset classes
        if (data.compatibility === 'EXCELLENT') {
            badge.classList.add('badge-success');
        } else if (data.compatibility === 'GOOD') {
            badge.classList.add('badge-info');
        } else if (data.compatibility === 'NEEDS_IMPROVEMENT') {
            badge.classList.add('badge-warning');
        } else {
            badge.classList.add('badge-danger');
        }
    }

    const scores = {
        'completeness': data.sectionCompletenessScore,
        'structure': data.structureScore,
        'keyword': data.keywordScore,
        'formatting': data.formattingScore,
        'readability': data.readabilityScore
    };

    for (const [key, val] of Object.entries(scores)) {
        const textEl = document.getElementById(`ats-${key}-score`);
        const fillEl = document.getElementById(`ats-${key}-fill`);
        if (textEl && fillEl) {
            textEl.textContent = `${val}%`;
            fillEl.style.width = `${val}%`;
            if (val >= 80) {
                fillEl.style.backgroundColor = 'var(--success)';
            } else if (val >= 60) {
                fillEl.style.backgroundColor = 'var(--secondary)';
            } else if (val >= 45) {
                fillEl.style.backgroundColor = 'var(--warning)';
            } else {
                fillEl.style.backgroundColor = 'var(--danger)';
            }
        }
    }
}

function renderMatchTab(data) {
    const resultsSection = document.getElementById('match-results-section');
    if (resultsSection) resultsSection.style.display = 'block';

    const pct = data.resumeMatchPercentage || 0;
    const pctValEl = document.getElementById('match-pct-value');
    if (pctValEl) pctValEl.textContent = `${pct}%`;
    
    const pctCard = document.querySelector('.match-percentage-card');
    if (pctCard) {
        pctCard.style.borderColor = pct >= 80 ? 'var(--success)' : pct >= 55 ? 'var(--secondary)' : 'var(--danger)';
    }

    const skillVal = document.getElementById('match-skill-val');
    if (skillVal) skillVal.textContent = `${data.skillMatch}%`;
    const expVal = document.getElementById('match-experience-val');
    if (expVal) expVal.textContent = `${data.experienceMatch}%`;
    const eduVal = document.getElementById('match-education-val');
    if (eduVal) eduVal.textContent = `${data.educationMatch}%`;
    const compVal = document.getElementById('match-compatibility-val');
    if (compVal) compVal.textContent = `${data.overallCompatibilityScore}%`;

    const skillsContainer = document.getElementById('missing-skills-tags');
    if (skillsContainer) {
        skillsContainer.innerHTML = '';
        if (data.missingSkills && data.missingSkills.length > 0) {
            data.missingSkills.forEach(skill => {
                const span = document.createElement('span');
                span.className = 'missing-tag skill-missing';
                span.textContent = skill;
                skillsContainer.appendChild(span);
            });
        } else {
            skillsContainer.innerHTML = '<span class="text-muted small">No missing skills detected! Perfect match.</span>';
        }
    }

    const kwContainer = document.getElementById('missing-keywords-tags');
    if (kwContainer) {
        kwContainer.innerHTML = '';
        if (data.missingKeywords && data.missingKeywords.length > 0) {
            data.missingKeywords.forEach(kw => {
                const span = document.createElement('span');
                span.className = 'missing-tag kw-missing';
                span.textContent = kw;
                kwContainer.appendChild(span);
            });
        } else {
            kwContainer.innerHTML = '<span class="text-muted small">No missing keywords detected.</span>';
        }
    }
}

function renderAiTab(data) {
    const structEl = document.getElementById('ai-structure-text');
    if (structEl) structEl.textContent = data.structure || '--';
    const formEl = document.getElementById('ai-formatting-text');
    if (formEl) formEl.textContent = data.formatting || '--';
    const toneEl = document.getElementById('ai-tone-text');
    if (toneEl) toneEl.textContent = data.professionalTone || '--';
    const readEl = document.getElementById('ai-readability-text');
    if (readEl) readEl.textContent = data.readability || '--';
    const lenEl = document.getElementById('ai-length-text');
    if (lenEl) lenEl.textContent = data.length || '--';

    const renderList = (elId, list, fallbackText = "None detected.") => {
        const el = document.getElementById(elId);
        if (!el) return;
        el.innerHTML = '';
        const isNotOk = list && list.length > 0 && 
                        list[0] !== "No duplicate sections found." && 
                        list[0] !== "No major spelling errors detected." && 
                        list[0] !== "Grammar looks solid.";
        if (isNotOk) {
            list.forEach(item => {
                const li = document.createElement('li');
                li.textContent = item;
                el.appendChild(li);
            });
        } else {
            const li = document.createElement('li');
            li.className = 'text-muted italic';
            li.style.listStyleType = 'none';
            li.textContent = list && list.length > 0 ? list[0] : fallbackText;
            el.appendChild(li);
        }
    };

    renderList('ai-duplicates-list', data.duplicateContent, "No duplicate content detected.");
    renderList('ai-spelling-list', data.spelling, "No spelling issues found.");
    renderList('ai-grammar-list', data.grammar, "No grammar issues found.");
    renderList('ai-weak-sentences-list', data.weakSentences, "No weak sentences detected.");
    renderList('ai-passive-voice-list', data.passiveVoice, "No passive voice usage detected.");
}

// ----------------------------------------------------
// HELPERS & DYNAMIC UTILITIES
// ----------------------------------------------------
function getFileIconClass(mimeType) {
    if (!mimeType) return 'fa-solid fa-file';
    if (mimeType.includes('pdf')) {
        return 'fa-solid fa-file-pdf';
    } else if (mimeType.includes('word') || mimeType.includes('msword')) {
        return 'fa-solid fa-file-word';
    }
    return 'fa-solid fa-file';
}

function formatDate(dateString) {
    if (!dateString) return 'Just now';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch(e) {
        return dateString;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function showNotification(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;
    
    const icon = type === 'success' ? 'fa-circle-check' : 
                 type === 'danger' ? 'fa-circle-exclamation' : 'fa-circle-info';
                 
    toast.innerHTML = `
        <i class="fa-solid ${icon}"></i>
        <span>${escapeHtml(message)}</span>
    `;
    
    document.body.appendChild(toast);
    
    // Animate enter
    setTimeout(() => toast.classList.add('show'), 50);
    
    // Animate exit
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Append styling for toast notifications dynamically to keep code clean
const style = document.createElement('style');
style.textContent = `
.toast-notification {
    position: fixed;
    bottom: 2rem;
    right: 2rem;
    background: var(--bg-card);
    backdrop-filter: blur(10px);
    border: 1px solid var(--border-color);
    padding: 1rem 1.5rem;
    border-radius: 12px;
    display: flex;
    align-items: center;
    gap: 0.8rem;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
    z-index: 2000;
    transform: translateY(20px);
    opacity: 0;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-notification.show {
    transform: translateY(0);
    opacity: 1;
}
.toast-notification.success {
    border-color: rgba(0, 230, 118, 0.3);
}
.toast-notification.success i {
    color: var(--success);
}
.toast-notification.danger {
    border-color: rgba(255, 23, 68, 0.3);
}
.toast-notification.danger i {
    color: var(--danger);
}
.toast-notification.info {
    border-color: rgba(0, 240, 255, 0.3);
}
.toast-notification.info i {
    color: var(--secondary);
}
`;
document.head.appendChild(style);
