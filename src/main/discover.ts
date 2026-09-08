import type { DiscoverSearch, GalleryImage, ModpackCard, ProjectDetails, ProjectType, SearchResult } from '../shared/types'
import { getSettings } from './store'

const MODRINTH = 'https://api.modrinth.com/v2'
const CURSEFORGE = 'https://api.curseforge.com/v1'
const USER_AGENT = 'TidalClient/0.1.0 (tidal-client)'

interface ModrinthHit {
  project_id: string
  project_type?: string
  slug: string
  title: string
  description: string
  icon_url: string | null
  downloads: number
  author: string
  categories: string[]
}

interface CurseForgeMod {
  id: number
  name: string
  slug: string
  summary: string
  downloadCount: number
  classId?: number
  logo?: { url: string }
  authors?: { name: string }[]
  categories?: { name: string }[]
  screenshots?: { url: string; title?: string }[]
}

function normalizeProjectType(value: string | undefined): ProjectType {
  if (value === 'mod' || value === 'resourcepack' || value === 'shader' || value === 'modpack') return value
  return 'modpack'
}

function curseForgeType(classId?: number): ProjectType {
  if (classId === 6) return 'mod'
  if (classId === 12) return 'resourcepack'
  if (classId === 6552) return 'shader'
  return 'modpack'
}

const PAGE_SIZE = 24

function typeFacets(projectType?: ProjectType | 'all'): string[] {
  if (projectType && projectType !== 'all') return [`project_type:${projectType}`]
  return ['project_type:modpack', 'project_type:mod', 'project_type:resourcepack']
}

async function searchModrinth(query: string, options: DiscoverSearch): Promise<ModpackCard[]> {
  const offset = options.offset ?? 0
  const facets: string[][] = [typeFacets(options.projectType)]
  if (options.gameVersion) facets.push([`versions:${options.gameVersion}`])
  const params = new URLSearchParams({
    query,
    limit: String(PAGE_SIZE),
    offset: String(offset),
    index: query.trim() ? 'relevance' : 'follows',
    facets: JSON.stringify(facets)
  })
  const res = await fetch(`${MODRINTH}/search?${params.toString()}`, {
    headers: { 'User-Agent': USER_AGENT }
  })
  if (!res.ok) throw new Error(`Modrinth search failed (${res.status})`)
  const data = (await res.json()) as { hits: ModrinthHit[] }
  return data.hits.map((hit) => ({
    id: `modrinth:${hit.project_id}`,
    source: 'modrinth' as const,
    projectType: normalizeProjectType(hit.project_type),
    title: hit.title,
    description: hit.description,
    iconUrl: hit.icon_url ?? '',
    downloads: hit.downloads,
    author: hit.author,
    categories: hit.categories ?? [],
    slug: hit.slug
  }))
}

async function searchCurseForgeClass(
  query: string,
  apiKey: string,
  classId: number,
  offset: number,
  gameVersion?: string
): Promise<ModpackCard[]> {
  const params = new URLSearchParams({
    gameId: '432',
    classId: String(classId),
    pageSize: String(PAGE_SIZE),
    index: String(offset),
    sortField: '2',
    sortOrder: 'desc'
  })
  if (query.trim()) params.set('searchFilter', query.trim())
  if (gameVersion) params.set('gameVersion', gameVersion)

  const res = await fetch(`${CURSEFORGE}/mods/search?${params.toString()}`, {
    headers: {
      Accept: 'application/json',
      'x-api-key': apiKey,
      'User-Agent': USER_AGENT
    }
  })
  if (!res.ok) throw new Error(`CurseForge search failed (${res.status})`)
  const data = (await res.json()) as { data: CurseForgeMod[] }
  return data.data.map((mod) => ({
    id: `curseforge:${mod.id}`,
    source: 'curseforge' as const,
    projectType: curseForgeType(mod.classId ?? classId),
    title: mod.name,
    description: mod.summary,
    iconUrl: mod.logo?.url ?? '',
    downloads: mod.downloadCount,
    author: mod.authors?.[0]?.name ?? 'Unknown',
    categories: (mod.categories ?? []).map((c) => c.name),
    slug: mod.slug,
    curseProjectId: mod.id
  }))
}

async function searchCurseForge(query: string, apiKey: string, options: DiscoverSearch): Promise<ModpackCard[]> {
  if (!apiKey.trim()) {
    throw new Error('Add a CurseForge API key in Settings to enable CurseForge search.')
  }
  const offset = options.offset ?? 0
  const classIds =
    options.projectType === 'mod'
      ? [6]
      : options.projectType === 'resourcepack'
        ? [12]
        : options.projectType === 'shader'
          ? [6552]
          : options.projectType === 'modpack'
            ? [4471]
            : [4471, 6, 12]
  const groups = await Promise.all(
    classIds.map((classId) => searchCurseForgeClass(query, apiKey, classId, offset, options.gameVersion))
  )
  return groups.flat()
}

export async function searchModpacks(query: string, options: DiscoverSearch = {}): Promise<SearchResult> {
  const settings = getSettings()
  const tasks: Promise<ModpackCard[]>[] = []
  const errors: string[] = []
  const offset = options.offset ?? 0

  if (!settings.modrinthEnabled && !settings.curseforgeEnabled) {
    return { hits: [], offset, pageSize: PAGE_SIZE, hasMore: false, error: 'Turn on Modrinth or CurseForge support to browse content.' }
  }

  if (settings.modrinthEnabled) tasks.push(searchModrinth(query, options))
  if (settings.curseforgeEnabled) tasks.push(searchCurseForge(query, settings.curseforgeApiKey, options))

  const settled = await Promise.allSettled(tasks)
  const hits: ModpackCard[] = []
  for (const result of settled) {
    if (result.status === 'fulfilled') hits.push(...result.value)
    else errors.push(result.reason instanceof Error ? result.reason.message : String(result.reason))
  }

  hits.sort((a, b) => b.downloads - a.downloads)
  return {
    hits,
    offset,
    pageSize: PAGE_SIZE,
    hasMore: hits.length >= PAGE_SIZE,
    error: hits.length === 0 && errors.length ? errors.join(' ') : errors[0]
  }
}

export async function fetchProjectDetails(card: ModpackCard): Promise<ProjectDetails> {
  if (card.source === 'modrinth') {
    const id = card.slug ?? card.id.replace(/^modrinth:/, '')
    const res = await fetch(`${MODRINTH}/project/${id}`, { headers: { 'User-Agent': USER_AGENT } })
    if (!res.ok) throw new Error('Could not load Modrinth project details')
    const project = (await res.json()) as {
      title: string
      description: string
      body: string
      icon_url: string | null
      project_type: string
      gallery?: Array<{ url: string; title?: string; featured?: boolean }>
    }
    const gallery: GalleryImage[] = (project.gallery ?? []).map((item) => ({
      url: item.url,
      title: item.title
    }))
    return {
      card: {
        ...card,
        title: project.title,
        description: project.description,
        iconUrl: project.icon_url ?? card.iconUrl,
        projectType: normalizeProjectType(project.project_type)
      },
      bodyHtml: project.body || project.description,
      gallery
    }
  }

  const settings = getSettings()
  if (!settings.curseforgeApiKey) throw new Error('A CurseForge API key is required for this project.')
  const projectId = card.curseProjectId ?? Number(card.id.replace(/^curseforge:/, ''))
  const descRes = await fetch(`${CURSEFORGE}/mods/${projectId}/description`, {
    headers: {
      Accept: 'application/json',
      'x-api-key': settings.curseforgeApiKey,
      'User-Agent': USER_AGENT
    }
  })
  const modRes = await fetch(`${CURSEFORGE}/mods/${projectId}`, {
    headers: {
      Accept: 'application/json',
      'x-api-key': settings.curseforgeApiKey,
      'User-Agent': USER_AGENT
    }
  })
  if (!modRes.ok) throw new Error('Could not load CurseForge project details')
  const modJson = (await modRes.json()) as { data: CurseForgeMod }
  const descJson = descRes.ok ? ((await descRes.json()) as { data: string }) : { data: card.description }
  const gallery: GalleryImage[] = (modJson.data.screenshots ?? []).map((shot) => ({
    url: shot.url,
    title: shot.title
  }))
  return {
    card: {
      ...card,
      title: modJson.data.name,
      description: modJson.data.summary,
      iconUrl: modJson.data.logo?.url ?? card.iconUrl,
      projectType: curseForgeType(modJson.data.classId)
    },
    bodyHtml: descJson.data || card.description,
    gallery
  }
}

export async function fetchModrinthVersion(
  projectId: string,
  match?: { gameVersion?: string; loader?: string }
): Promise<{
  files: { url: string; filename: string; primary: boolean }[]
}> {
  const params = new URLSearchParams()
  if (match?.gameVersion) params.set('game_versions', JSON.stringify([match.gameVersion]))
  if (match?.loader === 'vanilla' || match?.loader === 'minecraft') {
    params.set('loaders', JSON.stringify(['fabric']))
  } else if (match?.loader) {
    params.set('loaders', JSON.stringify([match.loader]))
  }
  const query = params.toString()
  const res = await fetch(`${MODRINTH}/project/${projectId}/version${query ? `?${query}` : ''}`, {
    headers: { 'User-Agent': USER_AGENT }
  })
  if (!res.ok) throw new Error('Could not load Modrinth versions')
  const versions = (await res.json()) as Array<{
    files: { url: string; filename: string; primary: boolean }[]
  }>
  const latest = versions[0]
  if (!latest) {
    if (match?.gameVersion) {
      throw new Error(
        `No ${match.loader && match.loader !== 'vanilla' ? match.loader + ' ' : ''}build for Minecraft ${match.gameVersion}. Pick a matching instance or another version.`
      )
    }
    throw new Error('This Modrinth project has no versions')
  }
  return latest
}

export async function fetchCurseForgeLatestFile(
  projectId: number,
  apiKey: string,
  match?: { gameVersion?: string }
): Promise<{ id: number; downloadUrl: string; fileName: string }> {
  const params = new URLSearchParams({ pageSize: '20' })
  if (match?.gameVersion) params.set('gameVersion', match.gameVersion)
  const res = await fetch(`${CURSEFORGE}/mods/${projectId}/files?${params.toString()}`, {
    headers: {
      Accept: 'application/json',
      'x-api-key': apiKey,
      'User-Agent': USER_AGENT
    }
  })
  if (!res.ok) throw new Error('Could not load CurseForge files')
  const data = (await res.json()) as {
    data: { id: number; downloadUrl: string | null; fileName: string }[]
  }
  const file = data.data.find((item) => item.downloadUrl) ?? data.data[0]
  if (!file) throw new Error('This CurseForge project has no files')
  let downloadUrl = file.downloadUrl
  if (!downloadUrl) {
    const urlRes = await fetch(`${CURSEFORGE}/mods/${projectId}/files/${file.id}/download-url`, {
      headers: { Accept: 'application/json', 'x-api-key': apiKey, 'User-Agent': USER_AGENT }
    })
    const urlJson = (await urlRes.json()) as { data: string }
    downloadUrl = urlJson.data
  }
  if (!downloadUrl) throw new Error('This CurseForge file has no download URL')
  return { id: file.id, downloadUrl, fileName: file.fileName }
}

export async function curseForgeFileDownloadUrl(
  projectId: number,
  fileId: number,
  apiKey: string
): Promise<string> {
  const res = await fetch(`${CURSEFORGE}/mods/${projectId}/files/${fileId}/download-url`, {
    headers: { Accept: 'application/json', 'x-api-key': apiKey, 'User-Agent': USER_AGENT }
  })
  if (!res.ok) throw new Error(`CurseForge file ${fileId} is unavailable`)
  const json = (await res.json()) as { data: string }
  return json.data
}
