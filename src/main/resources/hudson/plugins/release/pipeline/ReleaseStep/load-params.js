Behaviour.specify('.release-load-params', 'release-load-params', 0, function(e) {
    e.addEventListener('blur', function() {
        const dataSpan = document.querySelector('.release-params-data');
        const div = document.getElementById('params');

        const url = dataSpan.dataset.descriptorUrl + '/parameters?job=' +
            encodeURIComponent(this.value) +
            '&context=' + encodeURIComponent(dataSpan.dataset.context);

        fetch(url)
            .then(rsp => {
                if (rsp.ok) {
                    return rsp.text();
                }
                throw new Error(rsp.statusText);
            })
            .then(responseText => {
                div.innerHTML = responseText;
                Behaviour.applySubtree(div);
            })
            .catch(error => {
                div.innerHTML = '<b>ERROR</b>: Failed to load parameter definitions: ' + error.message;
            });
    });
});
