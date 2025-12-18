/**
 * Mail Admin Console - 主 JavaScript 文件
 */

// ===== 主题切换 =====
function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const newTheme = currentTheme === 'light' ? 'dark' : 'light';
    html.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
}

// 初始化主题
(function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
})();

// ===== 侧边栏切换 =====
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.toggle('open');
}

// 点击外部关闭侧边栏
document.addEventListener('click', function(e) {
    const sidebar = document.getElementById('sidebar');
    const toggle = document.querySelector('.sidebar-toggle');
    
    if (sidebar && sidebar.classList.contains('open') && 
        !sidebar.contains(e.target) && 
        !toggle.contains(e.target)) {
        sidebar.classList.remove('open');
    }
});

// ===== 模态框管理 =====
function closeModal(event) {
    if (event.target === event.currentTarget) {
        closeModalContainer();
    }
}

function closeModalContainer() {
    const container = document.getElementById('modal-container');
    if (container) {
        container.innerHTML = '';
    }
}

// ESC 键关闭模态框
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeModalContainer();
    }
});

// ===== HTMX 事件处理 =====

// 请求开始时显示加载指示器
document.body.addEventListener('htmx:beforeRequest', function(evt) {
    // 可以添加额外的加载状态处理
});

// 请求完成后的处理
document.body.addEventListener('htmx:afterSwap', function(evt) {
    // 如果返回的内容是 toast，将其移动到 toast-container
    const toast = evt.detail.target.querySelector('.toast');
    if (toast) {
        const container = document.getElementById('toast-container');
        if (container) {
            container.appendChild(toast);
            // 5秒后自动移除
            setTimeout(() => toast.remove(), 5000);
        }
    }
});

// 处理重定向
document.body.addEventListener('htmx:beforeOnLoad', function(evt) {
    // 如果响应是重定向，手动跳转
    const xhr = evt.detail.xhr;
    if (xhr.status >= 300 && xhr.status < 400) {
        const redirectUrl = xhr.getResponseHeader('Location');
        if (redirectUrl) {
            window.location.href = redirectUrl;
        }
    }
});

// 域名保存成功后刷新表格
document.body.addEventListener('domain-saved', function(evt) {
    closeModalContainer();
    htmx.ajax('GET', '/domains/table', { target: '#domain-table-container' });
});

// 用户保存成功后刷新表格
document.body.addEventListener('user-saved', function(evt) {
    closeModalContainer();
    htmx.ajax('GET', '/users/table', { target: '#user-table-container' });
});

// 密码重置成功
document.body.addEventListener('password-reset', function(evt) {
    closeModalContainer();
    showToast('success', '密码重置成功');
});

// ===== Toast 通知 =====
function showToast(type, message) {
    const container = document.getElementById('toast-container');
    if (!container) return;
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            ${type === 'success' 
                ? '<circle cx="12" cy="12" r="10"/><polyline points="16 10 11 15 8 12"/>'
                : '<circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>'
            }
        </svg>
        <span>${message}</span>
        <button onclick="this.parentElement.remove()">×</button>
    `;
    
    container.appendChild(toast);
    
    // 自动移除
    setTimeout(() => toast.remove(), 5000);
}

// ===== 确认对话框增强 =====
document.body.addEventListener('htmx:confirm', function(evt) {
    // 使用自定义样式的确认对话框
    const message = evt.detail.question;
    if (message && !confirm(message)) {
        evt.preventDefault();
    }
});

// ===== 表单验证增强 =====
document.addEventListener('invalid', function(e) {
    e.preventDefault();
    const input = e.target;
    input.classList.add('error');
    
    // 显示错误信息
    let errorSpan = input.nextElementSibling;
    if (!errorSpan || !errorSpan.classList.contains('error-message')) {
        errorSpan = document.createElement('span');
        errorSpan.className = 'error-message';
        input.parentNode.insertBefore(errorSpan, input.nextSibling);
    }
    errorSpan.textContent = input.validationMessage;
}, true);

// 输入时清除错误状态
document.addEventListener('input', function(e) {
    const input = e.target;
    if (input.classList.contains('error')) {
        input.classList.remove('error');
        const errorSpan = input.nextElementSibling;
        if (errorSpan && errorSpan.classList.contains('error-message')) {
            errorSpan.remove();
        }
    }
}, true);

// ===== 全选功能 =====
document.addEventListener('change', function(e) {
    if (e.target.id === 'select-all') {
        const checkboxes = document.querySelectorAll('tbody input[type="checkbox"]');
        checkboxes.forEach(cb => cb.checked = e.target.checked);
    }
});

// ===== 配额计算辅助函数 =====
function updateQuotaBytes() {
    const value = document.getElementById('quotaValue').value;
    const unit = document.getElementById('quotaUnit').value;
    document.getElementById('quotaBytes').value = value * unit;
}

// ===== 格式化函数 =====
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(date) {
    return new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).format(new Date(date));
}

// ===== 键盘快捷键 =====
document.addEventListener('keydown', function(e) {
    // Ctrl/Cmd + K: 聚焦搜索框
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        const searchInput = document.querySelector('.search-input-wrapper input');
        if (searchInput) searchInput.focus();
    }
});

// ===== 页面加载动画 =====
document.addEventListener('DOMContentLoaded', function() {
    document.body.classList.add('loaded');
    
    // 为统计卡片添加入场动画
    const statCards = document.querySelectorAll('.stat-card');
    statCards.forEach((card, index) => {
        card.style.animationDelay = `${index * 0.1}s`;
        card.classList.add('animate-in');
    });
});

// ===== 动态数字动画 =====
function animateNumber(element, endValue, duration = 1000) {
    const startValue = 0;
    const startTime = performance.now();
    
    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // 使用 easeOutQuart 缓动函数
        const easeProgress = 1 - Math.pow(1 - progress, 4);
        const currentValue = Math.round(startValue + (endValue - startValue) * easeProgress);
        
        element.textContent = currentValue.toLocaleString();
        
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }
    
    requestAnimationFrame(update);
}

// 初始化数字动画
document.addEventListener('DOMContentLoaded', function() {
    const statValues = document.querySelectorAll('.stat-value');
    statValues.forEach(el => {
        const value = parseInt(el.textContent.replace(/,/g, ''));
        if (!isNaN(value)) {
            animateNumber(el, value);
        }
    });
});

console.log('🚀 Mail Admin Console 已加载');

