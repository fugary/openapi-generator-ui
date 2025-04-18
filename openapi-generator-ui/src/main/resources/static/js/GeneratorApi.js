export const Utils = (function () {
    /**
     * 设置值
     * @param key
     * @param data
     * @returns {boolean}
     */
    const setData = (key, data) => {
        try {
            localStorage.setItem(key, JSON.stringify(data));
            return true;
        } catch (error) {
            console.error(`Store.set 错误 [key=${key}]:`, error);
            return false;
        }
    };
    /**
     * 获取值
     * @param key
     * @returns {Object}
     */
    const getData = (key) => {
        try {
            const data = localStorage.getItem(key);
            if (data) {
                return JSON.parse(data);
            }
        } catch (error) {
            console.error(`Store.set 错误 [key=${key}]:`, error);
            return false;
        }
    }
    /**
     * 删除值
     * @param key
     * @returns {boolean}
     */
    const removeData = (key) => {
        try {
            localStorage.removeItem(key);
            return true;
        } catch (error) {
            console.error(`Store.remove 错误 [key=${key}]:`, error);
            return false;
        }
    };
    /**
     * 下载链接点击
     * @param downloadUrl
     */
    const $downloadWithLinkClick = (downloadUrl) => {
        const downloadLink = document.createElement('a')
        downloadLink.href = downloadUrl
        downloadLink.download = 'download'
        downloadLink.target = '_blank'
        downloadLink.click()
    };
    return {setData, getData, removeData, $downloadWithLinkClick}
})();

const {$downloadWithLinkClick, getData, removeData, setData} = Utils;

const {ref, watch} = Vue

/**
 * 地址
 * @type {[{name: string, url: string}]}
 */
export const supportedUrls = [{
    url: 'https://api.openapi-generator.tech/api',
    home: 'https://api.openapi-generator.tech',
    name: 'OpenAPI Generator Stable',
    supportFilter: true
}, {
    url: 'https://api-latest-master.openapi-generator.tech/api',
    home: 'https://api-latest-master.openapi-generator.tech/',
    name: 'OpenAPI Generator Latest Master'
}];

export const generatorModes = [{
    path: '/gen/clients',
    name: 'Client'
}, {
    path: '/gen/servers',
    name: 'Server'
}]

/**
 * 加载支持的语言
 * @param baseUrl
 * @param path
 * @param config
 * @returns {Promise<any>}
 */
export const loadClientLanguages = ({baseUrl, path}, config = {}) => {
    return fetch(baseUrl + path, Object.assign({method: 'GET'}, config))
        .then(response => response.json())
}
/**
 * 加载配置信息
 * @param baseUrl
 * @param path
 * @param language
 * @param config
 * @returns {Promise<*>}
 */
export const loadLanguageConfig = ({baseUrl, path, language}, config = {}) => {
    return fetch(`${baseUrl}${path}/${language}`, Object.assign({method: 'GET'}, config))
        .then(response => response.json())
}
/**
 * 后台过滤operationIds
 * @param data
 * @param config
 * @returns {Promise<any>}
 */
export const filterApi = (data, config = {}) => {
    return fetch('/filterApi', {
        method: 'POST', body: JSON.stringify(data),
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(response => response.json()).then(data => {
        if (data.success) {
            return data.resultData;
        } else {
            return Promise.reject(data.message);
        }
    });
}
/**
 * 生成代码
 * @param baseUrl
 * @param path
 * @param language
 * @param body
 * @param config
 * @returns {Promise<*>}
 */
export const newGenerateCode = ({baseUrl, path, language}, body, config = {}) => {
    let targetUrl = `/proxy${path}/${language}` // 服务端代理发送
    const headers = {
        'simple-api-target-url': baseUrl,
        'Content-Type': 'application/json'
    }
    return fetch(targetUrl, Object.assign({headers, body, method: 'POST'}, config))
}
const LANGUAGE_CONFIG_KEY = 'open-api-generator-language-config'
/**
 * 初始化配置数据
 */
export const useLanguageOptions = (openAPI, apiTags, errorRef) => {
    const languages = ref([]);
    const languageConfig = ref({});
    const languageOptions = ref([]);
    const lastLanguageModel = ref(getData(LANGUAGE_CONFIG_KEY));
    const defaultModel = {
        _path: generatorModes[0].path,
        _generatorUrl: supportedUrls[0].url,
        _language: 'java',
        config: {}
    };
    const languageModel = ref(lastLanguageModel.value || {...defaultModel});
    const loading = ref(false);
    const reInitLanguage = () => {
        loadClientLanguages({
            path: languageModel.value._path,
            baseUrl: languageModel.value._generatorUrl
        }).then(data => {
            languages.value = data
            if (languageModel.value._language && !languages.value.find(lang => lang === languageModel.value._language)) {
                if (languageModel.value._path === generatorModes[0].path) {
                    languageModel.value._language = 'java'
                } else if (languageModel.value._path === generatorModes[1].path) {
                    languageModel.value._language = 'spring'
                }
            }
        })
    };
    const reInitLanguageConfig = (overwrite) => {
        loadLanguageConfig({
            path: languageModel.value._path,
            baseUrl: languageModel.value._generatorUrl,
            language: languageModel.value._language
        }).then(data => {
            languageConfig.value = data
            calcLanguageOptions(overwrite)
        })
    }
    const calcLanguageOptions = (overwrite) => {
        if (overwrite) {
            languageModel.value.config = {}
        }
        languageOptions.value = Object.keys(languageConfig.value).map(key => {
            const config = languageConfig.value[key]
            const tooltip = config.description
            const option = {
                prop: key,
                label: key,
                value: config.default,
                tooltip
            }
            option.type = 'input'
            if (config.type === 'boolean') {
                option.type = 'switch'
                option.value = config.default === 'true'
            } else if (config.enum) {
                option.type = 'select'
                option.options = Object.keys(config.enum)
                    .map(key => ({value: key, label: key + ' - ' + config.enum[key]}))
            }
            if (option.value && overwrite) {
                languageModel.value.config[key] = option.value
            }
            return option
        });
    };
    watch(() => languageModel.value._language, () => reInitLanguageConfig(true));
    watch(() => [languageModel.value._generatorUrl, languageModel.value._path], reInitLanguage);
    watch(apiTags, () => {
        reInitLanguage();
        reInitLanguageConfig(!lastLanguageModel.value);
    });
    const resetLanguageConfig = () => {
        removeData(LANGUAGE_CONFIG_KEY);
        lastLanguageModel.value = null
        languageModel.value = {...defaultModel};
        reInitLanguage();
        reInitLanguageConfig(true);
    };
    const saveLanguageConfig = () => {
        lastLanguageModel.value = {...languageModel.value}
        setData(LANGUAGE_CONFIG_KEY, lastLanguageModel.value)
    };
    const doGenerateCode = async () => {
        const operationIds = apiTags.value.flatMap(apiTag => apiTag.operations)
            .filter(operation => operation.checked)
            .map(operation => operation.operationId);
        const currentUrlConf = supportedUrls.find(urlConf => urlConf.url === languageModel.value._generatorUrl);
        loading.value = true;
        errorRef.value = null;
        let filter = {};
        let openApiStr = openAPI.value;
        if (operationIds.length) {
            if (currentUrlConf.supportFilter) {
                filter = {openapiNormalizer: operationIds.length ? [`FILTER=operationId:${operationIds.join('|')}`] : []}
            } else {
                openApiStr = await filterApi({
                    openAPI: openAPI.value,
                    operationIds
                }).catch(err => errorRef.value = err)
                    .finally((() => loading.value = false))
            }
        }
        newGenerateCode({
            path: languageModel.value._path,
            baseUrl: languageModel.value._generatorUrl,
            language: languageModel.value._language
        }, JSON.stringify(Object.assign({
            spec: JSON.parse(openApiStr),
            options: languageModel.value.config
        }, filter))).then(async response => {
            if (response.ok) {
                const data = await response.json();
                lastLanguageModel.value = {...languageModel.value};
                setData(LANGUAGE_CONFIG_KEY, lastLanguageModel.value);
                if (data.link) {
                    const link = data.link.replace('http://', 'https://')
                    $downloadWithLinkClick(link)
                }
            } else {
                errorRef.value = await response.text();
            }
        }).finally(() => loading.value = false);
    }
    return {
        errorRef,
        lastLanguageModel,
        languageModel,
        languages,
        languageOptions,
        loading,
        resetLanguageConfig,
        saveLanguageConfig,
        doGenerateCode
    }
}
