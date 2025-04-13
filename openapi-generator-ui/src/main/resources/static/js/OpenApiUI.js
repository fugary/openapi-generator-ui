import {generatorModes, supportedUrls, useLanguageOptions} from './GeneratorApi.js'

const {createApp, ref} = Vue

const METHOD_MAP = {
    GET: 'primary',
    POST: 'success',
    PUT: 'warning',
    DELETE: 'danger'
};

createApp({
    setup() {
        const apiParam = ref(initialApiParam);
        const apiTags = ref(initialApiTags);
        const openAPI = ref(initialOpenAPI);
        const submitForm = event => {
            const form = event.target
            if (!form.checkValidity()) {
                event.preventDefault()
                event.stopPropagation()
            }
            form.classList.add('was-validated')
        };
        const {
            languageModel,
            languages,
            languageOptions,
            doGenerateCode,
            errorRef
        } = useLanguageOptions(!!apiTags.value, openAPI)
        return {
            apiParam,
            apiTags,
            methodMap: METHOD_MAP,
            languageModel,
            languages,
            languageOptions,
            supportedUrls,
            generatorModes,
            errorRef,
            submitForm,
            doGenerateCode
        }
    }
}).directive('custom-tooltip', {
    mounted: (el, binding) => {
        new bootstrap.Tooltip(el, {
            title: binding.value || ''
        })
        el.style.cursor = 'pointer';
    }
}).mount('#app')