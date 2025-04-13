import {$downloadWithLinkClick} from "./Utils.js";

const {ref, watch} = Vue

/**
 * 地址
 * @type {[{name: string, url: string}]}
 */
export const supportedUrls = [{
    url: 'https://api.openapi-generator.tech/api',
    home: 'https://api.openapi-generator.tech',
    name: 'OpenAPI Generator'
}, {
    url: 'https://generator.swagger.io/api',
    home: 'https://generator.swagger.io/',
    name: 'Swagger Generator'
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
        .then(response => response.json())
}
/**
 * 初始化配置数据
 */
export const useLanguageOptions = (init, openAPI) => {
    const languages = ref([])
    const languageConfig = ref({})
    const languageOptions = ref([])
    const languageModel = ref({
        _path: generatorModes[0].path,
        _generatorUrl: supportedUrls[0].url,
        _language: init ? 'java' : '',
        config: {}
    })
    const errorRef = ref()
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
    const calcLanguageOptions = () => {
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
            if (config.default !== null) {
                languageModel.value.config[key] = option.value
            }
            return option
        })
    };
    watch(() => languageModel.value._language, (language) => {
        if (language) {
            loadLanguageConfig({
                path: languageModel.value._path,
                baseUrl: languageModel.value._generatorUrl,
                language
            }).then(data => {
                languageConfig.value = data
                languageModel.config = {}
                calcLanguageOptions()
            })
        }
    }, {immediate: true});
    watch(() => languageModel.value._generatorUrl, reInitLanguage)
    watch(() => languageModel.value._path, reInitLanguage)
    if (init) {
        reInitLanguage();
    }
    const doGenerateCode = () => {
        newGenerateCode({
            path: languageModel.value._path,
            baseUrl: languageModel.value._generatorUrl,
            language: languageModel.value._language
        }, JSON.stringify({
            spec: JSON.parse(openAPI.value),
            options: languageModel.value.config
        })).then(data => {
            console.log('==============data', data)
            if (data.link) {
                const link = data.link.replace('http://', 'https://')
                $downloadWithLinkClick(link)
            }
        }, err => {
            errorRef.value = err.error
        })
    }
    return {
        errorRef,
        languageModel,
        languages,
        languageOptions,
        doGenerateCode
    }
}
