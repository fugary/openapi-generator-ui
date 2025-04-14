import {generatorModes, supportedUrls, useLanguageOptions} from './GeneratorApi.js'

const {createApp, ref, watch} = Vue

const METHOD_MAP = {
    GET: 'primary',
    POST: 'success',
    PUT: 'warning',
    DELETE: 'danger'
};

/**
 * 提交表单
 * @param apiParam
 * @param fileRef
 * @returns {(function(*): void)|*}
 */
const useSubmitForm = () => {
    const apiParam = ref({type: 'url', url: 'https://petstore.swagger.io/v2/swagger.json'});
    const apiTags = ref([]);
    const openAPI = ref();
    const fileRef = ref();
    const loading = ref(false);
    const checkedOperations = ref([]);
    watch(apiTags, () => {
        checkedOperations.value = apiTags.value.flatMap(apiTag => apiTag.operations).filter(operation => operation.checked);
        apiTags.value.forEach(apiTag => {
            apiTag.checkedCount = apiTag.operations?.filter(operation => operation.checked)?.length || 0;
        });
    }, {deep: true});
    const submitForm = function (event) {
        const form = event.target
        form.classList.remove('was-validated');
        if (!form.checkValidity()) {
            form.classList.add('was-validated');
        } else {
            const formData = new FormData();
            if (apiParam.value.type === 'file') {
                formData.append("file", fileRef.value.files[0]);
            }
            Object.entries(apiParam.value).forEach(([key, value]) => formData.append(key, value));
            loading.value = true;
            fetch('/loadApi', {method: 'POST', body: formData})
                .then(response => response.json())
                .then(data => {
                    openAPI.value = data.resultData;
                    apiTags.value = data.addons?.apiTags || [];
                }).finally(() => loading.value = false);
        }
        event.preventDefault();
        event.stopPropagation();
    };
    return {
        apiParam,
        apiTags,
        openAPI,
        fileRef,
        loading,
        checkedOperations,
        submitForm
    }
};

createApp({
    setup() {
        const {apiParam, apiTags, openAPI, fileRef, submitForm, loading, checkedOperations} = useSubmitForm();
        const {
            languageModel,
            lastLanguageModel,
            languages,
            languageOptions,
            loading: generateLoading,
            errorRef,
            resetLanguageConfig,
            saveLanguageConfig,
            doGenerateCode
        } = useLanguageOptions(openAPI, apiTags)
        return {
            apiParam,
            openAPI,
            apiTags,
            fileRef,
            checkedOperations,
            methodMap: METHOD_MAP,
            lastLanguageModel,
            languageModel,
            languages,
            languageOptions,
            supportedUrls,
            generatorModes,
            errorRef,
            submitForm,
            loading,
            generateLoading,
            resetLanguageConfig,
            saveLanguageConfig,
            doGenerateCode
        }
    }
}).directive('custom-tooltip', {
    mounted: (el, binding) => {
        new bootstrap.Tooltip(el, {
            title: binding.value || '',
            customClass: 'custom-tooltip'
        })
        el.style.cursor = 'pointer';
    }
}).mount('#app')
