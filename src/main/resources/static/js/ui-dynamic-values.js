(function () {
    function clampPercent(value) {
        const number = Number.parseFloat(value);
        if (Number.isNaN(number)) return 0;
        return Math.min(100, Math.max(0, number));
    }

    function applyDynamicValues() {
        document.querySelectorAll('[data-meter-rate]').forEach((element) => {
            element.style.width = clampPercent(element.dataset.meterRate) + '%';
        });

        document.querySelectorAll('.score-ring[data-score]').forEach((element) => {
            element.style.setProperty('--score', clampPercent(element.dataset.score));
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', applyDynamicValues);
    } else {
        applyDynamicValues();
    }
}());