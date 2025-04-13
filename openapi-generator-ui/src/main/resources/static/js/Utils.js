/**
 * 设置值
 * @param key
 * @param data
 * @returns {boolean}
 */
export const setData = (key, data) => {
    try {
        localStorage.setItem(key, JSON.stringify(data));
        return true;
    } catch (error) {
        console.error(`Store.set 错误 [key=${key}]:`, error);
        return false;
    }
}
/**
 * 获取值
 * @param key
 * @param data
 * @returns {boolean}
 */
export const getData = (key, data) => {
    try {
        localStorage.setItem(key, JSON.stringify(data));
        return true;
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
export const removeData = (key) => {
    try {
        localStorage.removeItem(key);
        return true;
    } catch (error) {
        console.error(`Store.remove 错误 [key=${key}]:`, error);
        return false;
    }
}

/**
 * 下载链接点击
 * @param downloadUrl
 */
export const $downloadWithLinkClick = (downloadUrl) => {
    const downloadLink = document.createElement('a')
    downloadLink.href = downloadUrl
    downloadLink.download = 'download'
    downloadLink.click()
}