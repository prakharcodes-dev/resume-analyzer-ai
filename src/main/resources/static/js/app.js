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
    resumes: [],
    activeTemplate: 'ats',
    selectedTmplSource: 'profile'
};

// ----------------------------------------------------
// INITIALIZATION
// ----------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    initThemeToggle();
    initRouting();
    initSearch();
    initUpload();
    initProfileActions();
    initComparisonAndCoverLetter();
    initTemplates();
    initLinkedInAnalyzer();
    initGitHubAnalyzer();
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
            if (targetView === 'resumes' || targetView === 'compare' || targetView === 'cover-letter' || targetView === 'templates') {
                loadResumes();
                if (targetView === 'templates') renderTemplatePreview();
            } else if (targetView === 'profile') {
                loadProfile();
            } else if (targetView === 'linkedin') {
                autoFillLinkedInForm(false);
            } else if (targetView === 'github') {
                autoFillGitHubForm(false);
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
// SEARCH FUNCTIONALITY
// ----------------------------------------------------
function initSearch() {
    const searchInput = document.getElementById('top-search-input');
    if (!searchInput) return;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.trim().toLowerCase();
        updateResumesUI(query);
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
        const searchInput = document.getElementById('top-search-input');
        const query = searchInput ? searchInput.value.trim().toLowerCase() : '';
        updateResumesUI(query);
        populateDropdowns();
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

    // Set Welcome Display & Top Header to Guest User consistently
    const firstName = prof.fullName ? prof.fullName.split(' ')[0] : 'Guest';
    document.getElementById('welcome-name').textContent = firstName || 'Guest User';
    document.getElementById('user-display-name').textContent = 'Guest User';

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
function updateResumesUI(filterQuery = '') {
    const recentContainer = document.getElementById('recent-resumes-list');
    const tableBody = document.getElementById('resumes-table-body');

    const cleanQuery = (filterQuery || '').trim().toLowerCase();

    // Helper to check if resume matches query
    const matchesQuery = (resume) => {
        if (!cleanQuery) return true;
        if (resume.fileName && resume.fileName.toLowerCase().includes(cleanQuery)) return true;
        if (resume.parseStatus && resume.parseStatus.toLowerCase().includes(cleanQuery)) return true;
        if (resume.parsedContent && resume.parsedContent.toLowerCase().includes(cleanQuery)) return true;
        return false;
    };

    const filteredResumes = state.resumes.filter(matchesQuery);

    // 1. Dashboard View (Recent 3 Resumes matching filter)
    recentContainer.innerHTML = '';
    const recents = filteredResumes.slice(0, 3);
    
    if (recents.length === 0) {
        recentContainer.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-folder-open"></i>
                <p>${cleanQuery ? 'No matching resumes found.' : 'No resumes uploaded yet.'}</p>
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

    // 2. Full History View Table (matching filter)
    tableBody.innerHTML = '';
    if (filteredResumes.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-5">
                    <i class="fa-solid fa-folder-open" style="font-size: 2rem; opacity: 0.3; display: block; margin-bottom: 0.5rem;"></i>
                    ${cleanQuery ? 'No resumes match your search query "' + escapeHtml(cleanQuery) + '".' : 'No resumes found. Go to the dashboard to upload your first resume!'}
                </td>
            </tr>`;
    } else {
        // Compute version numbers based on chronological order (oldest = v1.0, newest = vN.0)
        // state.resumes is ordered by uploadDate DESC, so length - index gives chronological version
        filteredResumes.forEach((resume, idx) => {
            const tr = document.createElement('tr');
            const fileIcon = getFileIconClass(resume.fileType);
            const statusClass = resume.parseStatus.toLowerCase();
            
            // Version tag computation
            const totalCount = state.resumes.length;
            const originalIndex = state.resumes.findIndex(r => r.id === resume.id);
            const versionNum = originalIndex !== -1 ? (totalCount - originalIndex) : (filteredResumes.length - idx);
            const versionTag = `v${versionNum}.0`;

            tr.innerHTML = `
                <td>
                    <span style="display: inline-flex; align-items: center; gap: 0.6rem;">
                        <i class="${fileIcon}" style="color: var(--primary); font-size: 1.1rem;"></i>
                        <strong>${escapeHtml(resume.fileName)}</strong>
                    </span>
                </td>
                <td><span class="version-badge"><i class="fa-solid fa-code-branch"></i> ${versionTag}</span></td>
                <td>${formatDate(resume.uploadDate)}</td>
                <td>${(resume.fileSize / 1024).toFixed(1)} KB</td>
                <td><span class="status-badge ${statusClass}">${resume.parseStatus}</span></td>
                <td class="actions">
                    <button class="btn btn-sm btn-outline" onclick="viewResumeDetails(${resume.id})">
                        <i class="fa-solid fa-magnifying-glass-chart"></i> View Report
                    </button>
                    <button class="btn btn-sm btn-primary" onclick="downloadReport(${resume.id}, 'strength')" title="Download Report">
                        <i class="fa-solid fa-download"></i> Download
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

    // Reset tabs to default (Strength Report View)
    const tabs = modal.querySelectorAll('.modal-tab');
    const tabContents = modal.querySelectorAll('.tab-content');
    tabs.forEach(t => t.classList.remove('active'));
    tabContents.forEach(c => c.classList.remove('active'));
    
    const defaultTab = modal.querySelector('.modal-tab[data-tab="tab-strength"]');
    if (defaultTab) defaultTab.classList.add('active');
    const defaultContent = document.getElementById('tab-strength');
    if (defaultContent) defaultContent.classList.add('active');

    // Bind modal footer download buttons
    const btnDlStrength = document.getElementById('btn-download-strength');
    const btnDlGrammar = document.getElementById('btn-download-grammar');
    const btnDlAts = document.getElementById('btn-download-ats');
    const btnDlAi = document.getElementById('btn-download-ai');

    if (btnDlStrength) btnDlStrength.onclick = () => downloadReport(id, 'strength');
    if (btnDlGrammar) btnDlGrammar.onclick = () => downloadReport(id, 'grammar');
    if (btnDlAts) btnDlAts.onclick = () => downloadReport(id, 'ats');
    if (btnDlAi) btnDlAi.onclick = () => downloadReport(id, 'ai');

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

        // Fetch Strength Report
        fetch(`/api/resumes/${id}/strength-report`)
            .then(res => res.json())
            .then(strengthData => renderStrengthTab(strengthData))
            .catch(err => console.error("Error loading Strength report:", err));

        // Fetch Grammar Report
        fetch(`/api/resumes/${id}/grammar-report`)
            .then(res => res.json())
            .then(grammarData => renderGrammarTab(grammarData))
            .catch(err => console.error("Error loading Grammar report:", err));

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

        // Fetch AI Suggestions
        fetch(`/api/resumes/${id}/suggestions`)
            .then(res => res.json())
            .then(sugData => renderSuggestionsTab(sugData))
            .catch(err => console.error("Error loading suggestions:", err));

        // Fetch Skills Analysis
        fetch(`/api/resumes/${id}/skills-analysis`)
            .then(res => res.json())
            .then(skillsData => renderSkillsAnalysisTab(skillsData))
            .catch(err => console.error("Error loading skills analysis:", err));

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

// ----------------------------------------------------
// THEME TOGGLE ENGINE
// ----------------------------------------------------
function initThemeToggle() {
    const btnToggle = document.getElementById('btn-theme-toggle');
    if (!btnToggle) return;

    const currentTheme = localStorage.getItem('theme') || 'light';
    if (currentTheme === 'dark') {
        document.body.classList.add('dark-mode');
        btnToggle.innerHTML = '<i class="fa-solid fa-sun"></i>';
    } else {
        document.body.classList.remove('dark-mode');
        btnToggle.innerHTML = '<i class="fa-solid fa-moon"></i>';
    }

    btnToggle.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        const isDark = document.body.classList.contains('dark-mode');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
        btnToggle.innerHTML = isDark ? '<i class="fa-solid fa-sun"></i>' : '<i class="fa-solid fa-moon"></i>';
    });
}

// ----------------------------------------------------
// ADVANCED FEATURES RENDERERS
// ----------------------------------------------------
function renderSuggestionsTab(data) {
    const fields = [
        'summary', 'experience', 'projects', 'skills', 'education', 
        'certifications', 'achievements', 'action-verbs', 
        'keyword-optimization', 'industry-improvements'
    ];
    fields.forEach(f => {
        const propName = f.replace(/-([a-z])/g, (g) => g[1].toUpperCase());
        const el = document.getElementById(`sug-${f}`);
        if (el) {
            el.textContent = data[propName] || 'No suggestions available.';
        }
    });
}

function renderSkillsAnalysisTab(data) {
    // 1. Skill Distribution (e.g. bar chart)
    const distContainer = document.getElementById('skills-dist-container');
    if (distContainer) {
        distContainer.innerHTML = '';
        const dist = data.distribution || {};
        for (const [key, val] of Object.entries(dist)) {
            const row = document.createElement('div');
            row.className = 'breakdown-card';
            row.style.padding = '0.75rem';
            row.innerHTML = `
                <div class="breakdown-info">
                    <span>${escapeHtml(key)}</span>
                    <strong>${val} skill(s)</strong>
                </div>
                <div class="ats-progress-bar">
                    <div class="fill" style="width: ${Math.min(val * 20, 100)}%; background-color: var(--secondary);"></div>
                </div>
            `;
            distContainer.appendChild(row);
        }
    }

    // 2. Skill Strength Graph
    const strengthContainer = document.getElementById('skills-strength-container');
    if (strengthContainer) {
        strengthContainer.innerHTML = '';
        const strength = data.strength || {};
        for (const [key, val] of Object.entries(strength)) {
            const row = document.createElement('div');
            row.className = 'breakdown-card';
            row.style.padding = '0.75rem';
            
            const color = val >= 80 ? 'var(--success)' : val >= 60 ? 'var(--secondary)' : val >= 45 ? 'var(--warning)' : 'var(--danger)';
            row.innerHTML = `
                <div class="breakdown-info">
                    <span>${escapeHtml(key)} Strength</span>
                    <strong>${val}%</strong>
                </div>
                <div class="ats-progress-bar">
                    <div class="fill" style="width: ${val}%; background-color: ${color};"></div>
                </div>
            `;
            strengthContainer.appendChild(row);
        }
    }

    // 3. Categorized skills tags list
    const categorizedContainer = document.getElementById('categorized-skills-container');
    if (categorizedContainer) {
        categorizedContainer.innerHTML = '';
        const cats = data.categories || {};
        const catLabels = {
            'languages': 'Programming Languages',
            'frameworks': 'Frameworks & Libraries',
            'databases': 'Databases',
            'cloud': 'Cloud Technologies',
            'devops': 'DevOps & CI/CD',
            'aiml': 'AI/ML Skills',
            'tools': 'Developer Tools',
            'softSkills': 'Soft Skills & Methods'
        };

        for (const [key, label] of Object.entries(catLabels)) {
            const list = cats[key] || [];
            if (list.length === 0) continue;

            const section = document.createElement('div');
            section.className = 'skill-category-item';
            
            let tagsHtml = '';
            list.forEach(skill => {
                tagsHtml += `<span class="report-skill">${escapeHtml(skill)}</span>`;
            });

            section.innerHTML = `
                <h6>${escapeHtml(label)}</h6>
                <div class="skill-category-tags">
                    ${tagsHtml}
                </div>
            `;
            categorizedContainer.appendChild(section);
        }
        if (categorizedContainer.children.length === 0) {
            categorizedContainer.innerHTML = '<p class="text-muted small">No categorized skills found.</p>';
        }
    }

    // 4. Missing Skills Gaps tags
    const missingContainer = document.getElementById('skills-missing-container');
    if (missingContainer) {
        missingContainer.innerHTML = '';
        if (data.missingSkills && data.missingSkills.length > 0) {
            data.missingSkills.forEach(skill => {
                const span = document.createElement('span');
                span.className = 'missing-tag skill-missing';
                span.textContent = skill;
                missingContainer.appendChild(span);
            });
        } else {
            missingContainer.innerHTML = '<span class="text-muted small">No major skill gaps identified!</span>';
        }
    }

    // 5. Recommended Skills tags
    const recContainer = document.getElementById('skills-recommended-container');
    if (recContainer) {
        recContainer.innerHTML = '';
        if (data.recommendedSkills && data.recommendedSkills.length > 0) {
            data.recommendedSkills.forEach(skill => {
                const span = document.createElement('span');
                span.className = 'missing-tag kw-missing';
                span.textContent = skill;
                recContainer.appendChild(span);
            });
        } else {
            recContainer.innerHTML = '<span class="text-muted small">Profile is well optimized.</span>';
        }
    }
}

// ----------------------------------------------------
// DOWNLOAD REPORT HANDLER
// ----------------------------------------------------
window.downloadReport = function(id, type = 'strength') {
    if (!id) return;
    const url = `/api/resumes/${id}/download-report/${type}`;
    const link = document.createElement('a');
    link.href = url;
    link.download = '';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showNotification(`Downloading ${type.toUpperCase()} report...`, 'info');
};

// ----------------------------------------------------
// RENDER STRENGTH TAB
// ----------------------------------------------------
function renderStrengthTab(data) {
    if (!data) return;

    // Rating Overview
    const ratingNode = data.resumeRating || {};
    const ratingScore = ratingNode.score || 0;
    const ratingGrade = ratingNode.grade || '--';

    const scoreEl = document.getElementById('strength-rating-score');
    if (scoreEl) scoreEl.textContent = ratingScore;
    const gradeEl = document.getElementById('strength-rating-grade');
    if (gradeEl) gradeEl.textContent = ratingGrade;
    const bigEl = document.getElementById('str-rating-big');
    if (bigEl) bigEl.textContent = `${ratingScore} / 100`;
    const labelEl = document.getElementById('str-rating-label');
    if (labelEl) labelEl.textContent = `Grade: ${ratingGrade}`;

    // 1. Resume Strengths
    const strList = document.getElementById('str-strengths-list');
    if (strList) {
        strList.innerHTML = '';
        if (data.strengths && data.strengths.length > 0) {
            data.strengths.forEach(str => {
                const li = document.createElement('li');
                li.innerHTML = `<i class="fa-solid fa-check text-success mr-2"></i> ${escapeHtml(str)}`;
                strList.appendChild(li);
            });
        } else {
            strList.innerHTML = '<li class="text-muted">No strengths evaluated.</li>';
        }
    }

    // 2. Weaknesses
    const weakList = document.getElementById('str-weaknesses-list');
    if (weakList) {
        weakList.innerHTML = '';
        if (data.weaknesses && data.weaknesses.length > 0) {
            data.weaknesses.forEach(w => {
                const li = document.createElement('li');
                li.innerHTML = `<i class="fa-solid fa-triangle-exclamation text-danger mr-2"></i> ${escapeHtml(w)}`;
                weakList.appendChild(li);
            });
        } else {
            weakList.innerHTML = '<li class="text-muted">No major weaknesses identified.</li>';
        }
    }

    // 3. Missing Sections
    const missingContainer = document.getElementById('str-missing-sections-tags');
    if (missingContainer) {
        missingContainer.innerHTML = '';
        if (data.missingSections && data.missingSections.length > 0) {
            data.missingSections.forEach(sec => {
                const span = document.createElement('span');
                span.className = 'missing-tag skill-missing';
                span.textContent = sec;
                missingContainer.appendChild(span);
            });
        } else {
            missingContainer.innerHTML = '<span class="text-muted small"><i class="fa-solid fa-circle-check text-success"></i> All standard resume sections present!</span>';
        }
    }

    // 4. ATS Readiness
    const atsNode = data.atsReadiness || {};
    const atsBadge = document.getElementById('str-ats-badge');
    if (atsBadge) {
        const rating = atsNode.rating || 'POOR';
        atsBadge.textContent = rating;
        atsBadge.className = 'badge ' + (rating === 'EXCELLENT' ? 'badge-success' : rating === 'GOOD' ? 'badge-info' : 'badge-warning');
    }
    const atsScoreText = document.getElementById('str-ats-score-text');
    if (atsScoreText) atsScoreText.textContent = `${atsNode.score || 0}% ATS Compatibility`;
    const atsSummary = document.getElementById('str-ats-summary-text');
    if (atsSummary) atsSummary.textContent = atsNode.summary || '';

    // 6. Improvement Suggestions
    const sugList = document.getElementById('str-suggestions-list');
    if (sugList) {
        sugList.innerHTML = '';
        if (data.improvementSuggestions && data.improvementSuggestions.length > 0) {
            data.improvementSuggestions.forEach(sug => {
                const li = document.createElement('li');
                li.innerHTML = `<i class="fa-solid fa-lightbulb text-warning mr-2"></i> ${escapeHtml(sug)}`;
                sugList.appendChild(li);
            });
        } else {
            sugList.innerHTML = '<li class="text-muted">No pending improvement suggestions.</li>';
        }
    }
}

// ----------------------------------------------------
// RENDER GRAMMAR & WRITING TAB
// ----------------------------------------------------
function renderGrammarTab(data) {
    if (!data) return;

    // 1. Grammar Errors
    const grmList = document.getElementById('grm-errors-list');
    if (grmList) {
        grmList.innerHTML = '';
        if (data.grammarErrors && data.grammarErrors.length > 0) {
            data.grammarErrors.forEach(err => {
                const li = document.createElement('li');
                li.textContent = err;
                grmList.appendChild(li);
            });
        } else {
            grmList.innerHTML = '<li class="text-muted italic">No grammar errors found.</li>';
        }
    }

    // 2. Spelling Mistakes
    const spellingContainer = document.getElementById('grm-spelling-container');
    if (spellingContainer) {
        spellingContainer.innerHTML = '';
        if (data.spellingMistakes && data.spellingMistakes.length > 0) {
            data.spellingMistakes.forEach(item => {
                const div = document.createElement('div');
                div.className = 'spelling-item-card mb-2';
                div.innerHTML = `
                    <p class="mb-1"><span class="text-danger">Found typo:</span> <strong>"${escapeHtml(item.found || '')}"</strong></p>
                    <p class="mb-0 text-success small"><i class="fa-solid fa-arrow-right"></i> Suggested correction: <strong>"${escapeHtml(item.suggestion || '')}"</strong></p>
                `;
                spellingContainer.appendChild(div);
            });
        } else {
            spellingContainer.innerHTML = '<p class="text-muted small mb-0"><i class="fa-solid fa-check text-success"></i> No spelling errors found.</p>';
        }
    }

    // 3. Readability
    const readNode = data.readability || {};
    const readScore = document.getElementById('grm-readability-score');
    if (readScore) readScore.textContent = `${readNode.score || 0} / 100`;
    const readLevel = document.getElementById('grm-readability-level');
    if (readLevel) readLevel.textContent = readNode.level || 'Moderate';
    const readGrade = document.getElementById('grm-readability-grade');
    if (readGrade) readGrade.textContent = readNode.grade || '';

    // 4. Writing Style
    const styleNode = data.writingStyle || {};
    const actionCount = document.getElementById('grm-action-count');
    if (actionCount) actionCount.textContent = styleNode.actionVerbsCount || 0;
    const passiveCount = document.getElementById('grm-passive-count');
    if (passiveCount) passiveCount.textContent = styleNode.passiveVoiceCount || 0;
    const styleRating = document.getElementById('grm-style-rating');
    if (styleRating) styleRating.textContent = styleNode.styleRating || '--';

    // 5. Professional Language & Tone
    const toneNode = data.professionalLanguage || {};
    const toneStatus = document.getElementById('grm-tone-status');
    if (toneStatus) toneStatus.textContent = toneNode.status || 'Professional';
    const formalScore = document.getElementById('grm-formal-score');
    if (formalScore) formalScore.textContent = `${toneNode.formalScore || 95}%`;
    const toneSummary = document.getElementById('grm-tone-summary');
    if (toneSummary) toneSummary.textContent = toneNode.summary || '';

    // 6. Sentence Structure
    const sentenceNode = data.sentenceStructure || {};
    const avgSentence = document.getElementById('grm-avg-sentence');
    if (avgSentence) avgSentence.textContent = `${sentenceNode.avgSentenceLength || 0} words`;
    const totalSentences = document.getElementById('grm-total-sentences');
    if (totalSentences) totalSentences.textContent = sentenceNode.totalSentences || 0;
    const flowRating = document.getElementById('grm-sentence-flow');
    if (flowRating) flowRating.textContent = sentenceNode.flowRating || '--';
}

// ----------------------------------------------------
// FEATURE 13 & 14: DROPDOWNS, COMPARISON & COVER LETTER
// ----------------------------------------------------
function populateDropdowns() {
    const v1Select = document.getElementById('compare-v1-select');
    const v2Select = document.getElementById('compare-v2-select');
    const clSelect = document.getElementById('cl-resume-select');

    if (!state.resumes || state.resumes.length === 0) {
        if (v1Select) v1Select.innerHTML = '<option value="">No uploaded resumes found</option>';
        if (v2Select) v2Select.innerHTML = '<option value="">No uploaded resumes found</option>';
        if (clSelect) clSelect.innerHTML = '<option value="">No uploaded resumes found</option>';
        return;
    }

    const totalCount = state.resumes.length;

    const tmplSelect = document.getElementById('tmpl-source-select');

    let optionsHtml = '<option value="">Select a resume version...</option>';
    let tmplOptionsHtml = '<option value="profile">Use Active Career Profile Data</option>';

    state.resumes.forEach((r, idx) => {
        const versionNum = totalCount - idx;
        const label = `${r.fileName} (v${versionNum}.0 - ${formatDate(r.uploadDate)})`;
        optionsHtml += `<option value="${r.id}">${escapeHtml(label)}</option>`;
        tmplOptionsHtml += `<option value="${r.id}">Uploaded Resume: ${escapeHtml(label)}</option>`;
    });

    if (v1Select) {
        const val1 = v1Select.value;
        v1Select.innerHTML = optionsHtml;
        if (val1) v1Select.value = val1;
        else if (state.resumes.length >= 2) v1Select.value = state.resumes[state.resumes.length - 1].id;
    }

    if (v2Select) {
        const val2 = v2Select.value;
        v2Select.innerHTML = optionsHtml;
        if (val2) v2Select.value = val2;
        else if (state.resumes.length >= 1) v2Select.value = state.resumes[0].id;
    }

    if (clSelect) {
        const valCl = clSelect.value;
        clSelect.innerHTML = optionsHtml;
        if (valCl) clSelect.value = valCl;
        else if (state.resumes.length >= 1) clSelect.value = state.resumes[0].id;
    }

    if (tmplSelect) {
        const valTmpl = tmplSelect.value;
        tmplSelect.innerHTML = tmplOptionsHtml;
        if (valTmpl) tmplSelect.value = valTmpl;
    }
}

function initComparisonAndCoverLetter() {
    // 1. Run Comparison Event Listener
    const btnCompare = document.getElementById('btn-run-comparison');
    if (btnCompare) {
        btnCompare.addEventListener('click', async () => {
            const id1 = document.getElementById('compare-v1-select').value;
            const id2 = document.getElementById('compare-v2-select').value;

            if (!id1 || !id2) {
                showNotification('Please select two resume versions to compare.', 'warning');
                return;
            }
            if (id1 === id2) {
                showNotification('Please select two DIFFERENT resume versions for comparison.', 'warning');
                return;
            }

            btnCompare.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Comparing...';
            btnCompare.disabled = true;

            try {
                const response = await fetch('/api/resumes/compare', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id1: parseInt(id1), id2: parseInt(id2) })
                });

                if (!response.ok) throw new Error('Comparison request failed');
                const data = await response.json();
                renderComparisonResults(data);
                showNotification('Resume comparison complete!', 'success');
            } catch (err) {
                showNotification('Error comparing resumes: ' + err.message, 'danger');
            } finally {
                btnCompare.innerHTML = '<i class="fa-solid fa-scale-balanced"></i> Compare Versions';
                btnCompare.disabled = false;
            }
        });
    }

    // 2. Generate Cover Letter Event Listener
    const btnGenerateCL = document.getElementById('btn-generate-cover-letter');
    if (btnGenerateCL) {
        btnGenerateCL.addEventListener('click', async () => {
            const resumeId = document.getElementById('cl-resume-select').value;
            const companyName = document.getElementById('cl-company-name').value.trim();
            const jobRole = document.getElementById('cl-job-role').value.trim();
            const jobDescription = document.getElementById('cl-job-desc').value.trim();

            if (!resumeId) {
                showNotification('Please select a source resume.', 'warning');
                return;
            }

            btnGenerateCL.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Generating Cover Letter...';
            btnGenerateCL.disabled = true;

            try {
                const response = await fetch('/api/resumes/cover-letter', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        resumeId: parseInt(resumeId),
                        companyName: companyName,
                        jobRole: jobRole,
                        jobDescription: jobDescription
                    })
                });

                if (!response.ok) throw new Error('Cover letter generation failed');
                const data = await response.json();
                const outputEl = document.getElementById('cover-letter-output');
                if (outputEl) {
                    outputEl.textContent = data.coverLetter || 'No cover letter content generated.';
                }
                showNotification('AI Cover Letter generated successfully!', 'success');
            } catch (err) {
                showNotification('Error generating cover letter: ' + err.message, 'danger');
            } finally {
                btnGenerateCL.innerHTML = '<i class="fa-solid fa-wand-magic-sparkles"></i> Generate AI Cover Letter';
                btnGenerateCL.disabled = false;
            }
        });
    }

    // 3. Copy Cover Letter
    const btnCopyCL = document.getElementById('btn-copy-cover-letter');
    if (btnCopyCL) {
        btnCopyCL.addEventListener('click', () => {
            const text = document.getElementById('cover-letter-output').textContent;
            if (!text || text.includes('Click "Generate AI Cover Letter"')) {
                showNotification('Generate a cover letter first to copy.', 'warning');
                return;
            }
            navigator.clipboard.writeText(text).then(() => {
                showNotification('Cover letter copied to clipboard!', 'info');
            }).catch(err => {
                showNotification('Failed to copy: ' + err.message, 'danger');
            });
        });
    }

    // 4. Download Cover Letter
    const btnDownloadCL = document.getElementById('btn-download-cover-letter');
    if (btnDownloadCL) {
        btnDownloadCL.addEventListener('click', () => {
            const text = document.getElementById('cover-letter-output').textContent;
            if (!text || text.includes('Click "Generate AI Cover Letter"')) {
                showNotification('Generate a cover letter first to download.', 'warning');
                return;
            }
            const blob = new Blob([text], { type: 'text/plain' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'Cover_Letter.txt';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
            showNotification('Cover Letter downloaded as text file!', 'info');
        });
    }
}

function renderComparisonResults(data) {
    const container = document.getElementById('compare-results-container');
    if (!container) return;

    container.style.display = 'block';

    const r1 = data.resume1 || {};
    const r2 = data.resume2 || {};
    const delta = data.atsScoreImprovement || 0;
    const rate = data.improvementPercentage || 0;

    // Delta Big Stat
    const deltaEl = document.getElementById('cmp-ats-delta');
    if (deltaEl) {
        deltaEl.textContent = (delta >= 0 ? '+' : '') + delta + '%';
        deltaEl.className = 'big-stat ' + (delta >= 0 ? 'text-green' : 'text-danger');
    }

    const detailEl = document.getElementById('cmp-ats-detail');
    if (detailEl) {
        detailEl.textContent = `${r1.fileName || 'Version 1'}: ${r1.atsScore || 0}% → ${r2.fileName || 'Version 2'}: ${r2.atsScore || 0}%`;
    }

    // Rate Big Stat
    const rateEl = document.getElementById('cmp-improvement-rate');
    if (rateEl) {
        rateEl.textContent = (rate >= 0 ? '+' : '') + rate + '%';
        rateEl.className = 'big-stat ' + (rate >= 0 ? 'text-green' : 'text-danger');
    }

    // Added Skills
    const addedSkills = data.addedSkills || [];
    const addedSkillsContainer = document.getElementById('cmp-added-skills-container');
    if (addedSkillsContainer) {
        addedSkillsContainer.innerHTML = '';
        if (addedSkills.length > 0) {
            addedSkills.forEach(s => {
                const span = document.createElement('span');
                span.className = 'added-skill-tag';
                span.textContent = '+ ' + s;
                addedSkillsContainer.appendChild(span);
            });
        } else {
            addedSkillsContainer.innerHTML = '<span class="text-muted small">No new skills added in comparison version.</span>';
        }
    }

    // Skill Growth Big Stat
    const growthEl = document.getElementById('cmp-skill-growth');
    if (growthEl) {
        growthEl.textContent = `+${addedSkills.length} Skills`;
    }

    // Removed Skills
    const removedSkills = data.removedSkills || [];
    const removedSkillsContainer = document.getElementById('cmp-removed-skills-container');
    if (removedSkillsContainer) {
        removedSkillsContainer.innerHTML = '';
        if (removedSkills.length > 0) {
            removedSkills.forEach(s => {
                const span = document.createElement('span');
                span.className = 'removed-skill-tag';
                span.textContent = '- ' + s;
                removedSkillsContainer.appendChild(span);
            });
        } else {
            removedSkillsContainer.innerHTML = '<span class="text-muted small">No skills removed or omitted.</span>';
        }
    }

    // Added Keywords
    const addedKw = data.addedKeywords || [];
    const addedKwContainer = document.getElementById('cmp-added-keywords-container');
    if (addedKwContainer) {
        addedKwContainer.innerHTML = '';
        if (addedKw.length > 0) {
            addedKw.forEach(k => {
                const span = document.createElement('span');
                span.className = 'added-skill-tag';
                span.textContent = '+ ' + k;
                addedKwContainer.appendChild(span);
            });
        } else {
            addedKwContainer.innerHTML = '<span class="text-muted small">No new keywords detected.</span>';
        }
    }

    // Removed Keywords
    const removedKw = data.removedKeywords || [];
    const removedKwContainer = document.getElementById('cmp-removed-keywords-container');
    if (removedKwContainer) {
        removedKwContainer.innerHTML = '';
        if (removedKw.length > 0) {
            removedKw.forEach(k => {
                const span = document.createElement('span');
                span.className = 'removed-skill-tag';
                span.textContent = '- ' + k;
                removedKwContainer.appendChild(span);
            });
        } else {
            removedKwContainer.innerHTML = '<span class="text-muted small">No keywords removed.</span>';
        }
    }
}

// ----------------------------------------------------
// FEATURE 17: RESUME TEMPLATES FUNCTIONALITY
// ----------------------------------------------------
function initTemplates() {
    const tmplCards = document.querySelectorAll('.template-card');
    tmplCards.forEach(card => {
        card.addEventListener('click', () => {
            tmplCards.forEach(c => c.classList.remove('active'));
            card.classList.add('active');
            const tmplId = card.getAttribute('data-template');
            state.activeTemplate = tmplId;

            const badgeMap = {
                ats: 'ATS Friendly Resume',
                professional: 'Professional Corporate Resume',
                modern: 'Modern Polished Resume',
                minimal: 'Minimalist Clean Resume',
                creative: 'Creative Portfolio Resume'
            };
            const badgeEl = document.getElementById('tmpl-active-badge');
            if (badgeEl) badgeEl.textContent = badgeMap[tmplId] || 'Resume Template';

            renderTemplatePreview();
        });
    });

    const tmplSelect = document.getElementById('tmpl-source-select');
    if (tmplSelect) {
        tmplSelect.addEventListener('change', (e) => {
            state.selectedTmplSource = e.target.value;
            renderTemplatePreview();
        });
    }

    const btnPrint = document.getElementById('btn-tmpl-print');
    if (btnPrint) {
        btnPrint.addEventListener('click', () => {
            window.print();
        });
    }

    const btnExportHtml = document.getElementById('btn-tmpl-export-html');
    if (btnExportHtml) {
        btnExportHtml.addEventListener('click', () => {
            const canvas = document.getElementById('tmpl-preview-canvas');
            if (!canvas) return;
            const content = canvas.innerHTML;
            const fullDoc = `<!DOCTYPE html><html><head><meta charset="UTF-8"><title>Resume Export</title></head><body>${content}</body></html>`;
            const blob = new Blob([fullDoc], { type: 'text/html' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `Resume_${state.activeTemplate}.html`;
            a.click();
            URL.revokeObjectURL(url);
            showNotification('Resume HTML exported successfully!', 'success');
        });
    }

    const btnCopyText = document.getElementById('btn-tmpl-copy-text');
    if (btnCopyText) {
        btnCopyText.addEventListener('click', () => {
            const canvas = document.getElementById('tmpl-preview-canvas');
            if (!canvas) return;
            const text = canvas.innerText;
            navigator.clipboard.writeText(text).then(() => {
                showNotification('Resume text copied to clipboard!', 'success');
            }).catch(() => {
                showNotification('Failed to copy text.', 'danger');
            });
        });
    }
}

async function renderTemplatePreview() {
    const canvas = document.getElementById('tmpl-preview-canvas');
    if (!canvas) return;

    canvas.innerHTML = '<div class="text-center p-5 text-muted"><i class="fa-solid fa-spinner fa-spin fa-2x mb-3"></i><br>Rendering Resume Template...</div>';

    try {
        const payload = {
            templateId: state.activeTemplate,
            resumeId: (state.selectedTmplSource && state.selectedTmplSource !== 'profile') ? parseInt(state.selectedTmplSource) : null
        };

        const response = await fetch('/api/templates/render', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error('Failed to render template');
        const data = await response.json();

        if (data && data.html) {
            canvas.innerHTML = data.html;
        } else {
            canvas.innerHTML = '<div class="alert alert-danger">Error rendering template content.</div>';
        }
    } catch (err) {
        console.error(err);
        canvas.innerHTML = '<div class="text-center p-5 text-muted">Error loading template preview. Please ensure backend server is running.</div>';
    }
}

// ----------------------------------------------------
// FEATURE 18: LINKEDIN PROFILE ANALYZER FUNCTIONALITY
// ----------------------------------------------------
function initLinkedInAnalyzer() {
    const btnAutofill = document.getElementById('btn-linkedin-autofill');
    if (btnAutofill) {
        btnAutofill.addEventListener('click', () => autoFillLinkedInForm(true));
    }

    const btnRun = document.getElementById('btn-run-linkedin-analysis');
    if (btnRun) {
        btnRun.addEventListener('click', runLinkedInAnalysis);
    }
}

function autoFillLinkedInForm(notify = false) {
    const p = state.profile;
    if (!p) return;

    const roleInput = document.getElementById('li-target-role');
    const headlineInput = document.getElementById('li-headline');
    const aboutInput = document.getElementById('li-about');
    const skillsInput = document.getElementById('li-skills');
    const certsInput = document.getElementById('li-certs');
    const expInput = document.getElementById('li-experience');
    const urlInput = document.getElementById('li-profile-url');

    if (roleInput && !roleInput.value) roleInput.value = 'Senior Software Engineer';
    if (urlInput && p.linkedinUrl) urlInput.value = p.linkedinUrl;
    if (headlineInput && !headlineInput.value) {
        headlineInput.value = `${p.fullName || 'Software Candidate'} | Full Stack Engineer & Tech Specialist`;
    }
    if (aboutInput && !aboutInput.value) {
        aboutInput.value = `Passionate and results-driven engineering professional with hands-on experience building scalable applications, designing robust architecture, and delivering high-quality software solutions.`;
    }

    if (skillsInput && p.skills) {
        try {
            const parsed = JSON.parse(p.skills);
            if (Array.isArray(parsed)) skillsInput.value = parsed.join(', ');
            else skillsInput.value = p.skills;
        } catch (e) {
            skillsInput.value = p.skills;
        }
    }

    if (expInput && p.experience) {
        try {
            const parsed = JSON.parse(p.experience);
            if (Array.isArray(parsed) && parsed.length > 0) {
                expInput.value = parsed.map(x => `• ${x.title || x.role || 'Role'} at ${x.company || 'Company'}: ${x.description || x.details || ''}`).join('\n');
            }
        } catch (e) {}
    }

    if (notify) showNotification('LinkedIn form auto-filled from your profile details!', 'success');
}

async function runLinkedInAnalysis() {
    const btnRun = document.getElementById('btn-run-linkedin-analysis');
    const resultsContainer = document.getElementById('linkedin-results-container');
    if (!btnRun || !resultsContainer) return;

    const payload = {
        targetRole: document.getElementById('li-target-role').value.trim() || 'Software Engineer',
        profileUrl: document.getElementById('li-profile-url').value.trim(),
        headline: document.getElementById('li-headline').value.trim(),
        about: document.getElementById('li-about').value.trim(),
        skills: document.getElementById('li-skills').value.trim(),
        certifications: document.getElementById('li-certs').value.trim(),
        experience: document.getElementById('li-experience').value.trim()
    };

    if (!payload.headline && !payload.about && !payload.skills) {
        showNotification('Please enter at least a Headline, About summary, or Skills to analyze.', 'warning');
        return;
    }

    btnRun.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Auditing Profile...';
    btnRun.disabled = true;

    try {
        const response = await fetch('/api/analyzer/linkedin', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error('LinkedIn audit failed');
        const data = await response.json();

        renderLinkedInResults(data);
        resultsContainer.style.display = 'block';
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    } catch (err) {
        console.error(err);
        showNotification('Error performing LinkedIn analysis.', 'danger');
    } finally {
        btnRun.innerHTML = '<i class="fa-solid fa-chart-pie"></i> Run LinkedIn Profile Audit';
        btnRun.disabled = false;
    }
}

function renderLinkedInResults(data) {
    document.getElementById('li-overall-score').textContent = data.overallScore || 0;
    document.getElementById('li-score-grade').textContent = data.grade || 'LinkedIn Audit Complete';

    const catGrid = document.getElementById('li-categories-grid');
    if (catGrid && data.categoryScores) {
        catGrid.innerHTML = '';
        Object.keys(data.categoryScores).forEach(catName => {
            const item = data.categoryScores[catName];
            const card = document.createElement('div');
            card.className = 'category-score-card';
            card.innerHTML = `
                <div class="cat-header">
                    <span class="cat-title">${escapeHtml(catName)}</span>
                    <span class="cat-badge ${item.status}">${item.status}</span>
                </div>
                <div class="cat-bar-wrapper">
                    <div class="cat-bar-fill" style="width: ${item.score}%;"></div>
                </div>
                <div class="cat-footer">
                    <span>Score: ${item.score}/100</span>
                    <span>Weight: ${item.weight}</span>
                </div>
            `;
            catGrid.appendChild(card);
        });
    }

    const probsList = document.getElementById('li-problems-list');
    if (probsList) {
        probsList.innerHTML = '';
        const problems = data.identifiedProblems || [];
        if (problems.length > 0) {
            problems.forEach(p => {
                const div = document.createElement('div');
                div.className = 'issue-item';
                div.innerHTML = `<i class="fa-solid fa-circle-exclamation text-danger"></i> <span>${escapeHtml(p)}</span>`;
                probsList.appendChild(div);
            });
        } else {
            probsList.innerHTML = '<div class="alert alert-success"><i class="fa-solid fa-circle-check"></i> No critical profile issues detected! Excellent positioning.</div>';
        }
    }

    const suggsList = document.getElementById('li-suggestions-list');
    if (suggsList) {
        suggsList.innerHTML = '';
        const suggestions = data.actionableSuggestions || [];
        suggestions.forEach(s => {
            const div = document.createElement('div');
            div.className = 'suggestion-item';
            div.innerHTML = `<i class="fa-solid fa-lightbulb text-warning"></i> <span>${escapeHtml(s)}</span>`;
            suggsList.appendChild(div);
        });
    }
}

// ----------------------------------------------------
// FEATURE 19: GITHUB PROFILE ANALYZER FUNCTIONALITY
// ----------------------------------------------------
function initGitHubAnalyzer() {
    const btnRun = document.getElementById('btn-run-github-analysis');
    if (btnRun) {
        btnRun.addEventListener('click', runGitHubAnalysis);
    }
}

function autoFillGitHubForm(notify = false) {
    const p = state.profile;
    if (!p) return;

    const usernameInput = document.getElementById('gh-username');
    const roleInput = document.getElementById('gh-target-role');

    if (usernameInput && p.githubUrl && !usernameInput.value) {
        usernameInput.value = p.githubUrl;
    }
    if (roleInput && !roleInput.value) {
        roleInput.value = 'Full Stack Engineer';
    }
}

async function runGitHubAnalysis() {
    const btnRun = document.getElementById('btn-run-github-analysis');
    const resultsContainer = document.getElementById('github-results-container');
    if (!btnRun || !resultsContainer) return;

    const username = document.getElementById('gh-username').value.trim();
    const targetRole = document.getElementById('gh-target-role').value.trim() || 'Software Engineer';

    if (!username) {
        showNotification('Please enter a GitHub username or profile URL.', 'warning');
        return;
    }

    btnRun.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Auditing GitHub Profile...';
    btnRun.disabled = true;

    try {
        const response = await fetch('/api/analyzer/github', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, targetRole })
        });

        if (!response.ok) throw new Error('GitHub analysis failed');
        const data = await response.json();

        renderGitHubResults(data);
        resultsContainer.style.display = 'block';
        resultsContainer.scrollIntoView({ behavior: 'smooth' });
    } catch (err) {
        console.error(err);
        showNotification('Error performing GitHub analysis.', 'danger');
    } finally {
        btnRun.innerHTML = '<i class="fa-brands fa-github-alt"></i> Audit GitHub Developer Profile';
        btnRun.disabled = false;
    }
}

function renderGitHubResults(data) {
    document.getElementById('gh-overall-score').textContent = data.overallScore || 0;
    document.getElementById('gh-score-grade').textContent = data.grade || 'GitHub Developer Audit';

    const bannerCard = document.getElementById('gh-user-banner');
    if (bannerCard && data.retrievedStats) {
        const s = data.retrievedStats;
        const liveBadge = data.isLiveApiData 
            ? '<span class="badge badge-success"><i class="fa-solid fa-wifi"></i> Live GitHub API Data</span>' 
            : '<span class="badge badge-warning"><i class="fa-solid fa-database"></i> Benchmark Analysis</span>';

        const avatarSrc = s.avatarUrl || 'https://github.com/identicons/guest.png';
        const topLangsStr = (s.topLanguages && s.topLanguages.length > 0) ? s.topLanguages.join(', ') : 'Languages N/A';

        bannerCard.innerHTML = `
            <img src="${escapeHtml(avatarSrc)}" alt="Avatar" class="gh-avatar" onerror="this.src='https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png'">
            <div class="gh-user-info">
                <div class="flex-header">
                    <h3>${escapeHtml(s.name || data.username)} (@${escapeHtml(data.username)})</h3>
                    ${liveBadge}
                </div>
                <p>${escapeHtml(s.bio || 'Public GitHub Developer Profile')}</p>
                <div class="gh-stats-pills">
                    <span class="gh-pill"><i class="fa-solid fa-book-bookmark text-primary"></i> ${s.publicRepos || 0} Repositories</span>
                    <span class="gh-pill"><i class="fa-solid fa-star text-warning"></i> ${s.totalStars || 0} Stars</span>
                    <span class="gh-pill"><i class="fa-solid fa-code-fork text-cyan"></i> ${s.totalForks || 0} Forks</span>
                    <span class="gh-pill"><i class="fa-solid fa-users text-green"></i> ${s.followers || 0} Followers</span>
                    <span class="gh-pill"><i class="fa-solid fa-code text-purple"></i> ${escapeHtml(topLangsStr)}</span>
                </div>
            </div>
        `;
    }

    const catGrid = document.getElementById('gh-categories-grid');
    if (catGrid && data.categoryScores) {
        catGrid.innerHTML = '';
        Object.keys(data.categoryScores).forEach(catName => {
            const item = data.categoryScores[catName];
            const card = document.createElement('div');
            card.className = 'category-score-card';
            card.innerHTML = `
                <div class="cat-header">
                    <span class="cat-title">${escapeHtml(catName)}</span>
                    <span class="cat-badge ${item.status}">${item.status}</span>
                </div>
                <div class="cat-bar-wrapper">
                    <div class="cat-bar-fill" style="width: ${item.score}%;"></div>
                </div>
                <div class="cat-footer">
                    <span>Score: ${item.score}/100</span>
                    <span>Weight: ${item.weight}</span>
                </div>
            `;
            catGrid.appendChild(card);
        });
    }

    const probsList = document.getElementById('gh-problems-list');
    if (probsList) {
        probsList.innerHTML = '';
        const issues = data.identifiedIssues || [];
        if (issues.length > 0) {
            issues.forEach(issue => {
                const div = document.createElement('div');
                div.className = 'issue-item';
                div.innerHTML = `<i class="fa-solid fa-circle-exclamation text-danger"></i> <span>${escapeHtml(issue)}</span>`;
                probsList.appendChild(div);
            });
        } else {
            probsList.innerHTML = '<div class="alert alert-success"><i class="fa-solid fa-circle-check"></i> Great job! No critical repository issues found.</div>';
        }
    }

    const suggsList = document.getElementById('gh-suggestions-list');
    if (suggsList) {
        suggsList.innerHTML = '';
        const suggestions = data.actionableSuggestions || [];
        suggestions.forEach(s => {
            const div = document.createElement('div');
            div.className = 'suggestion-item';
            div.innerHTML = `<i class="fa-solid fa-rocket text-success"></i> <span>${escapeHtml(s)}</span>`;
            suggsList.appendChild(div);
        });
    }
}
