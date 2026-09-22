import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import EvidenceList from './EvidenceList.vue'

describe('履约存证列表', () => {
  const global = {
    components: {
      ElEmpty: { template: '<div data-test="empty">暂无存证</div>' },
      ElImage: { props: ['src'], template: '<img :src="src" />' }
    }
  }

  it('没有证据时显示空状态', () => {
    const wrapper = mount(EvidenceList, { global })
    expect(wrapper.find('[data-test="empty"]').exists()).toBe(true)
  })

  it('按类型展示打卡、清单和轨迹，不把轨迹当成照片', () => {
    const wrapper = mount(EvidenceList, {
      props: {
        evidences: [
          { id: 1, type: 1, lng: 121.47, lat: 31.23, remark: '到达' },
          { id: 2, type: 2, checkItem: '换粮', imageUrl: '/proof.png' },
          { id: 3, type: 3, trackPoints: [
            { time: '2026-09-22 10:00:00' }, { time: '2026-09-22 10:15:00' }
          ] }
        ]
      },
      global
    })

    expect(wrapper.text()).toContain('坐标 121.47, 31.23')
    expect(wrapper.text()).toContain('作业清单 · 换粮')
    expect(wrapper.find('img').attributes('src')).toBe('/proof.png')
    expect(wrapper.text()).toContain('2 个轨迹点')
    expect(wrapper.text()).toContain('10:00:00 ~ 10:15:00')
  })
})
