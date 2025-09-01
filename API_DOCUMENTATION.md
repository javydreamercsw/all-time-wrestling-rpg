# All Time Wrestling RPG - API Documentation

## Overview

The All Time Wrestling RPG Management System provides a comprehensive REST API for managing wrestling promotions, wrestlers, matches, and storylines. This API supports both traditional wrestling management operations and advanced features like AI-powered match narration.

## 🚀 Quick Start

### Access the API Documentation

- **Interactive Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **API Information**: [http://localhost:8080/api/system/info](http://localhost:8080/api/system/info)

### Base URL

```
http://localhost:8080/api
```

## 📚 API Categories

### Core Management APIs

#### 🤼 Wrestler Management (`/api/wrestlers`)
- **CRUD Operations**: Create, read, update, delete wrestlers
- **Statistics**: Track wins, losses, championship reigns
- **Career Management**: Manage wrestler careers and storylines
- **Injury Tracking**: Monitor wrestler health and availability

#### 🎪 Show Management (`/api/shows`)
- **Show Scheduling**: Create and schedule wrestling shows
- **Calendar Integration**: View shows in calendar format
- **Template-Based Creation**: Use templates for consistent show formats
- **Season Organization**: Group shows into seasons

#### 🥊 Match System (`/api/matches`)
- **Match Booking**: Schedule matches between wrestlers
- **Results Tracking**: Record match outcomes and statistics
- **AI Narration**: Generate detailed match stories
- **Match Types**: Support various match stipulations

#### 🏆 Title Management (`/api/titles`)
- **Championship Tracking**: Manage title belts and lineages
- **Reign Management**: Track title reigns and statistics
- **Title Hierarchy**: Organize titles by importance and division

### Advanced Features

#### 🤖 AI Services (`/api/match-narration`)
- **Multiple Providers**: Google Gemini, OpenAI GPT, Anthropic Claude
- **Custom Contexts**: Detailed match scenarios and outcomes
- **Cost Estimation**: Track AI usage and costs
- **Rate Limiting**: Prevent API abuse and cost overruns

#### 🎭 Drama Events (`/api/drama-events`)
- **Storyline Management**: Create and track dramatic events
- **Wrestler Relationships**: Manage feuds and alliances
- **Event Processing**: Apply drama effects to wrestler stats

#### 🤕 Injury System (`/api/injuries`)
- **Injury Tracking**: Monitor wrestler injuries and recovery
- **Healing Mechanics**: Simulate recovery times
- **Availability Management**: Track wrestler availability for shows

#### 👥 Faction Management (`/api/factions`)
- **Team Organization**: Group wrestlers into factions
- **Stable Management**: Track faction relationships and dynamics

### Integration APIs

#### 📝 Notion Sync (`/api/sync/notion`)
- **Database Synchronization**: Sync with Notion databases
- **Manual Triggers**: Force sync operations
- **Status Monitoring**: Track sync progress and health
- **Entity Management**: Configure which entities to sync

#### 🔧 System APIs (`/api/system`)
- **Health Checks**: Monitor system status
- **API Information**: Get API metadata and capabilities
- **Statistics**: Usage metrics and performance data

## 🔐 Authentication

The API supports multiple authentication methods:

### JWT Bearer Token
```http
Authorization: Bearer <your-jwt-token>
```

### API Key
```http
X-API-Key: <your-api-key>
```

## 📊 Response Formats

All API responses use JSON format with consistent structure:

### Success Response
```json
{
"data": { ... },
"status": "success",
"timestamp": "2024-01-01T12:00:00Z"
}
```

### Error Response
```json
{
"error": "Error message",
"status": "error",
"code": 400,
"timestamp": "2024-01-01T12:00:00Z"
}
```

### Paginated Response
```json
{
"content": [...],
"pageable": {
	"page": 0,
	"size": 20,
	"sort": "name,asc"
},
"totalElements": 100,
"totalPages": 5
}
```

## 🎯 Common Use Cases

### 1. Create a Wrestling Show
```bash
curl -X POST http://localhost:8080/api/shows \
-H "Content-Type: application/json" \
-d '{
	"name": "Monday Night Wrestling",
	"description": "Weekly wrestling show",
	"showDate": "2024-01-15",
	"showTypeName": "Weekly"
}'
```

### 2. Generate Match Narration
```bash
curl -X POST http://localhost:8080/api/match-narration/narrate \
-H "Content-Type: application/json" \
-d '{
	"wrestlers": [
	{"name": "John Cena", "description": "The Cenation Leader"},
	{"name": "The Rock", "description": "The People's Champion"}
	],
	"matchType": {"matchType": "One on One"},
	"venue": {"name": "Madison Square Garden"},
	"audience": "Sold-out crowd of 20,000"
}'
```

### 3. Sync with Notion
```bash
curl -X POST http://localhost:8080/api/sync/notion/trigger/shows
```

### 4. Get API Health Status
```bash
curl http://localhost:8080/api/system/health
```

## 📈 Rate Limits

- **Standard Endpoints**: 1000 requests/minute
- **AI Narration**: 60 requests/minute (varies by provider)
- **Notion Sync**: 10 requests/minute

## 🔧 Configuration

### AI Services
Configure AI providers in `application.yml`:
```yaml
match-narration:
ai:
	max-output-tokens: 4000
	temperature: 0.8
	timeout-seconds: 90
```

### Notion Integration
```yaml
notion:
sync:
	enabled: true
	entities: [shows, wrestlers, templates]
	scheduler:
	enabled: true
	interval: 3600000  # 1 hour
```

## 🐛 Error Handling

The API uses standard HTTP status codes:

- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `429` - Rate Limited
- `500` - Internal Server Error
- `503` - Service Unavailable

## 📞 Support

- **GitHub Issues**: [Report bugs and request features](https://github.com/javydreamercsw/all-time-wrestling-rpg/issues)
- **Documentation**: [Wiki](https://github.com/javydreamercsw/all-time-wrestling-rpg/wiki)
- **API Status**: [http://localhost:8080/api/system/health](http://localhost:8080/api/system/health)

## 🔄 Versioning

The API follows semantic versioning. Current version: **1.0.0**

- Major version changes indicate breaking changes
- Minor version changes add new features
- Patch version changes include bug fixes

## 📝 Changelog

### Version 1.0.0
- Initial API release
- Core wrestler and show management
- AI-powered match narration
- Notion integration
- Comprehensive documentation
